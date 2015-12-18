package org.envirocar.obd.adapter;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDSupported;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.request.PIDCommand;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.ResponseParser;
import org.envirocar.obd.exception.AdapterFailedException;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.InvalidCommandResponseException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.StreamFinishedException;
import org.envirocar.obd.exception.UnmatchedResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Subscriber;

public abstract class SyncAdapter implements OBDAdapter {

    private static final Logger LOGGER = Logger.getLogger(SyncAdapter.class.getName());

    protected static final long ADAPTER_TRY_PERIOD = 20000;

    private static final char COMMAND_SEND_END = '\r';
    private static final char COMMAND_RECEIVE_END = '>';
    private static final char COMMAND_RECEIVE_SPACE = ' ';
    private static final int MAX_ERROR_PER_COMMAND = 5;

    private Set<Character> ignoredChars = new HashSet<>(Arrays.asList(COMMAND_RECEIVE_SPACE, COMMAND_SEND_END));
    private CommandExecutor commandExecutor;
    private ResponseParser parser = new ResponseParser();

    private Set<PID> supportedPIDs = new HashSet<>();

    private Map<PID, AtomicInteger> failureMap = new HashMap<>();
    private List<PIDCommand> requestCommands;
    private Queue<PIDCommand> commandRingBuffer = new ArrayDeque<>();
    private Queue<PIDSupported> pidSupportedCommands = new ArrayDeque<>(Arrays.asList(new PIDSupported[] {new PIDSupported(), new PIDSupported("20")}));


    @Override
    public Observable<Boolean> initialize(InputStream is, OutputStream os) {
        commandExecutor = new CommandExecutor(is, os, ignoredChars, COMMAND_RECEIVE_END, COMMAND_SEND_END);

        /**
         * create an observable that tries to verify the
         * connection based on response analysis
         */
        Observable<Boolean> obs = Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    boolean analyzedSuccessfully = false;

                    while (!subscriber.isUnsubscribed()) {
                        if (analyzedSuccessfully) {
                            /**
                             * a successful data connection has been established:
                             * retrieve the supported PIDs
                             */
                            PIDSupported pid = pidSupportedCommands.poll();
                            while (pid != null) {
                                commandExecutor.execute(pid);
                                byte[] resp = commandExecutor.retrieveLatestResponse();
                                try {
                                    supportedPIDs.addAll(pid.parsePIDs(resp));
                                } catch (InvalidCommandResponseException | NoDataReceivedException
                                        | UnmatchedResponseException | AdapterSearchingException e) {
                                    LOGGER.warn(e.getMessage(), e);
                                }
                                LOGGER.info("Currently supported PIDs: " + supportedPIDs.toString());
                                pid = pidSupportedCommands.poll();
                            }

                            subscriber.onNext(true);
                            subscriber.unsubscribe();
                        }
                        else {
                            BasicCommand cc = pollNextInitializationCommand();

                            if (cc == null) {
                                subscriber.onError(new AdapterFailedException(
                                        "All init commands sent, but could not verify connection"));
                                subscriber.unsubscribe();
                            }

                            //push the command to the output stream
                            commandExecutor.execute(cc);

                            //check if the command needs a response (most likely)
                            if (cc.awaitsResults()) {
                                byte[] resp = commandExecutor.retrieveLatestResponse();
                                analyzedSuccessfully = analyzeMetadataResponse(resp, cc);
                            }
                        }
                    }
                } catch (IOException | AdapterFailedException e) {
                    subscriber.onError(e);
                    subscriber.unsubscribe();
                } catch (StreamFinishedException e) {
                    LOGGER.warn("The stream was closed unexpectedly: "+e.getMessage());
                    subscriber.onCompleted();
                    subscriber.unsubscribe();
                }
            }
        });

        return obs;
    }

    @Override
    public Observable<DataResponse> observe() {
        return Observable.create(new Observable.OnSubscribe<DataResponse>() {

            @Override
            public void call(Subscriber<? super DataResponse> subscriber) {

                /**
                 * prepare all pending data commands
                 */
                preparePendingCommands();

                PIDCommand latestCommand = null;
                byte[] bytes = null;
                while (!subscriber.isUnsubscribed()) {
                    try {
                        latestCommand = pollNextCommand();
                        LOGGER.debug("Sending command " + (latestCommand != null ? latestCommand.getPid().toString() : "n/a"));

                        /**
                         * write the next pending command
                         */
                        if (latestCommand != null) {
                            commandExecutor.execute(latestCommand);
                        }

                        /**
                         * read the next incoming response
                         */
                        bytes = commandExecutor.retrieveLatestResponse();

                        DataResponse response = parser.parse(preProcess(bytes));

                        if (response != null) {
                            LOGGER.debug("isUnsubscribed? " + subscriber.isUnsubscribed());
                            subscriber.onNext(response);
                        }
                    } catch (IOException e) {
                        subscriber.onError(e);
                        subscriber.unsubscribe();
                    } catch (AdapterFailedException e) {
                        subscriber.onError(e);
                        subscriber.unsubscribe();
                    } catch (StreamFinishedException e) {
                        LOGGER.info("Stream finished: "+ e.getMessage());
                        subscriber.onCompleted();
                        subscriber.unsubscribe();
                    } catch (AdapterSearchingException e) {
                        LOGGER.warn("Adapter still searching: " + e.getMessage());
                    } catch (NoDataReceivedException e) {
                        LOGGER.warn("No data received: " + e.getMessage());
                        increaseFailureCount(latestCommand.getPid());
                    } catch (InvalidCommandResponseException e) {
                        LOGGER.warn("Received InvalidCommandResponseException: " + e.getCommand());
                        increaseFailureCount(PIDUtil.fromString(e.getCommand()));
                    } catch (UnmatchedResponseException e) {
                        LOGGER.warn("Unmatched response: " + e.getMessage());
                    }

                }

            }
        });
    }

    protected PIDCommand pollNextCommand() throws AdapterFailedException {
        if (this.commandRingBuffer.isEmpty()) {
            throw new AdapterFailedException("No available commands left in the buffer");
        }

        PIDCommand cmd = commandRingBuffer.poll();

        if (cmd != null) {
            if (!checkIsBlacklisted(cmd.getPid())) {
                /**
                 * not blacklisted: add it back as the last element of the ring
                 */
                commandRingBuffer.offer(cmd);
                return cmd;
            }
            else {
                /**
                 * blacklisted: do not re-add it and return the next candidate
                 */
                return pollNextCommand();
            }
        }

        return cmd;
    }

    protected void increaseFailureCount(PID command) {
        if (command == null) {
            return;
        }

        if (this.failureMap.containsKey(command)) {
            this.failureMap.get(command).getAndIncrement();
        }
        else {
            AtomicInteger ai = new AtomicInteger(1);
            this.failureMap.put(command, ai);
        }
    }

    protected List<PIDCommand> defaultCycleCommands() {
        if (requestCommands == null) {
            requestCommands = new ArrayList<>();

            addIfSupported(PID.SPEED);
            addIfSupported(PID.MAF);
            addIfSupported(PID.RPM);
            addIfSupported(PID.INTAKE_MAP);
            addIfSupported(PID.INTAKE_AIR_TEMP);
            addIfSupported(PID.CALCULATED_ENGINE_LOAD);
            addIfSupported(PID.TPS);
            addIfSupported(PID.O2_LAMBDA_PROBE_1_VOLTAGE);
            addIfSupported(PID.O2_LAMBDA_PROBE_1_CURRENT);
        }

        return requestCommands;
    }

    protected void addIfSupported(PID pid) {
        if (supportedPIDs == null || supportedPIDs.isEmpty()) {
            requestCommands.add(PIDUtil.instantiateCommand(pid));
        }
        else if (supportedPIDs.contains(pid)) {
            requestCommands.add(PIDUtil.instantiateCommand(pid));
        }
        else {
            LOGGER.info("PID "+pid.toString()+" not supported. Skipping.");
        }
    }

    private void preparePendingCommands() {
        commandRingBuffer = new ArrayDeque<>();

        for (PIDCommand cmd : providePendingCommands()) {
            if (cmd != null) {
                commandRingBuffer.offer(cmd);
            }
        }
    }

    private boolean checkIsBlacklisted(PID pid) {
        return this.failureMap.containsKey(pid)  && this.failureMap.get(pid).get() > MAX_ERROR_PER_COMMAND;
    }

    @Override
    public long getExpectedInitPeriod() {
        return ADAPTER_TRY_PERIOD;
    }

    protected abstract BasicCommand pollNextInitializationCommand();

    protected abstract List<PIDCommand> providePendingCommands();

    /**
     *
     * @param response the raw data received
     * @param sentCommand the originating command
     * @return true if the adapter established a meaningful connection
     */
    protected abstract boolean analyzeMetadataResponse(byte[] response, BasicCommand sentCommand) throws AdapterFailedException;

    protected abstract byte[] preProcess(byte[] bytes);
}
