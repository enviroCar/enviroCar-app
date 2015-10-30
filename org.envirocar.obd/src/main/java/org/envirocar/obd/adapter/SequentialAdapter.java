package org.envirocar.obd.adapter;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.obd.commands.EngineLoad;
import org.envirocar.obd.commands.IntakePressure;
import org.envirocar.obd.commands.IntakeTemperature;
import org.envirocar.obd.commands.MAF;
import org.envirocar.obd.commands.O2LambdaProbe;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.commands.RPM;
import org.envirocar.obd.commands.Speed;
import org.envirocar.obd.commands.TPS;
import org.envirocar.obd.commands.exception.AdapterSearchingException;
import org.envirocar.obd.commands.exception.NoDataReceivedException;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.ResponseParser;
import org.envirocar.obd.protocol.OBDConnector;
import org.envirocar.obd.protocol.exception.AdapterFailedException;
import org.envirocar.obd.protocol.exception.InvalidCommandResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

public abstract class SequentialAdapter implements OBDConnector {

    private static final Logger LOGGER = Logger.getLogger(SequentialAdapter.class.getName());
    private static final char COMMAND_SEND_END = '\r';
    private static final char COMMAND_RECEIVE_END = '>';
    private static final char COMMAND_RECEIVE_SPACE = ' ';
    private static final int MAX_ERROR_PER_COMMAND = 5;

    private Set<Character> ignoredChars = new HashSet<>(Arrays.asList(COMMAND_RECEIVE_SPACE, COMMAND_SEND_END));
    private CommandExecutor commandExecutor;
    private ResponseParser parser = new ResponseParser();

    private Map<PID, AtomicInteger> failureMap = new HashMap<>();

    public Observable<Boolean> initialize(InputStream is, OutputStream os) {
        commandExecutor = new CommandExecutor(is, os, ignoredChars, COMMAND_RECEIVE_END);

        Observable<Boolean> obs = Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                subscriber.onStart();

                while (!subscriber.isUnsubscribed()) {
                    for (CommonCommand cc : preparePendingCommands()) {
                        try {
                            commandExecutor.execute(cc);
                            if (cc.awaitsResults()) {
                                byte[] resp = commandExecutor.retrieveLatestResponse();
                                if (analyzeMetadataResponse(resp, cc)) {
                                    subscriber.onNext(true);
                                }
                            }
                        } catch (IOException e) {
                            subscriber.onError(e);
                        } catch (AdapterFailedException e) {
                            subscriber.onError(e);
                        }
                    }
                }

                subscriber.onCompleted();
            }
        });

        return obs;
    }

    public Observable<DataResponse> observe() {
        return Observable.create(new Observable.OnSubscribe<DataResponse>() {
            @Override
            public void call(Subscriber<? super DataResponse> subscriber) {
                subscriber.onStart();

                Subscription obs = commandExecutor.createRawByteObservable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe(new Observer<byte[]>() {
                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                //TODO switch over exceptions?
                                subscriber.onError(e);
                            }

                            @Override
                            public void onNext(byte[] bytes) {
                                try {
                                    DataResponse response = parser.parse(filter(bytes));

                                    if (response != null) {
                                        subscriber.onNext(response);
                                    }
                                } catch (AdapterSearchingException e) {
                                    LOGGER.warn("Adapter still searching: "+e.getMessage());
                                } catch (NoDataReceivedException e) {
                                    LOGGER.warn("No data received: "+e.getMessage());
                                } catch (InvalidCommandResponseException e) {
                                    increaseFailureCount(e.getCommand());
                                }
                            }
                        });
                subscriber.add(obs);

                while (!subscriber.isUnsubscribed()) {
                    for (CommonCommand cc : preparePendingCommands()) {
                        try {
                            commandExecutor.execute(cc);
                        } catch (IOException e) {
                            LOGGER.warn("IOException on command " + cc.getClass().getSimpleName(), e);
                            subscriber.onError(e);
                        }
                    }
                }

                subscriber.onCompleted();
            }
        });
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

    protected List<CommonCommand> defaultCycleCommands() {
        List<CommonCommand> requestCommands = new ArrayList<>();

        requestCommands.add(new Speed());
        requestCommands.add(new MAF());
        requestCommands.add(new RPM());
        requestCommands.add(new IntakePressure());
        requestCommands.add(new IntakeTemperature());
        requestCommands.add(new EngineLoad());
        requestCommands.add(new TPS());
        requestCommands.add(O2LambdaProbe.fromPIDEnum(PID.O2_LAMBDA_PROBE_1_VOLTAGE));
        requestCommands.add(O2LambdaProbe.fromPIDEnum(PID.O2_LAMBDA_PROBE_1_CURRENT));

        return Collections.unmodifiableList(requestCommands);
    }

    private List<CommonCommand> preparePendingCommands() {
        List<PID> pids = providePendingCommands();
        List<CommonCommand> result = new ArrayList<>();

        for (PID pid : pids) {
            if (!checkIsBlacklisted(pid))
            result.add(PIDUtil.instantiateCommand(pid));
        }

        return result;
    }

    private boolean checkIsBlacklisted(PID pid) {
        return this.failureMap.containsKey(pid)  && this.failureMap.get(pid).get() > MAX_ERROR_PER_COMMAND;
    }

    protected abstract List<PID> providePendingCommands();

    /**
     *
     * @param response the raw data received
     * @param sentCommand the originating command
     * @return true if the adapter established a meaningful connection
     */
    protected abstract boolean analyzeMetadataResponse(byte[] response, CommonCommand sentCommand) throws AdapterFailedException;

    public abstract boolean supportsDevice(String deviceName);

    protected abstract byte[] filter(byte[] bytes);
}
