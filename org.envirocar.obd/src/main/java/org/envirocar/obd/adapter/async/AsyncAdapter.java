package org.envirocar.obd.adapter.async;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.adapter.CommandExecutor;
import org.envirocar.obd.adapter.OBDAdapter;
import org.envirocar.obd.adapter.ResponseQuirkWorkaround;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.InvalidCommandResponseException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.StreamFinishedException;
import org.envirocar.obd.exception.UnmatchedResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * Created by matthes on 03.11.15.
 */
public abstract class AsyncAdapter implements OBDAdapter {

    private static Logger LOGGER = Logger.getLogger(AsyncAdapter.class);

    private static final long DEFAULT_NO_DATA_TIMEOUT = 15000; //*10 for debug
    private final char endOfLineOutput;
    private final char endOfLineInput;
    private CommandExecutor commandExecutor;
    private Subscription dataObservable;
    private AtomicBoolean quirkDisabled = new AtomicBoolean(false);

    public AsyncAdapter(char endOfLineOutput, char endOfLineInput) {
        this.endOfLineOutput = endOfLineOutput;
        this.endOfLineInput = endOfLineInput;
    }

    /**
     * use this method to disable a CommandExecutor quirk.
     * @see {@link #getQuirk()}
     */
    public void disableQuirk() {
        if (!quirkDisabled.getAndSet(true)) {
            commandExecutor.setQuirk(null);
        }
    }

    @Override
    public Observable<Boolean> initialize(InputStream is, OutputStream os) {
        this.commandExecutor = new CommandExecutor(is, os, Collections.emptySet(), this.endOfLineInput, this.endOfLineOutput);
        this.commandExecutor.setQuirk(getQuirk());

        /**
         *
         */
        Observable<Boolean> observable = Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                while (!subscriber.isUnsubscribed()) {
                    /**
                     * poll the next possible command
                     */
                    BasicCommand cmd = pollNextCommand();
                    if (cmd != null) {
                        try {
                            commandExecutor.execute(cmd);
                        } catch (IOException e) {
                            subscriber.onError(e);
                            subscriber.unsubscribe();
                        }
                    }

                    try {
                        byte[] response = commandExecutor.retrieveLatestResponse();

                        processResponse(response);

                        if (hasEstablishedConnection()) {
                            subscriber.onNext(true);
                            subscriber.onCompleted();
                        }

                    } catch (IOException e) {
                        subscriber.onError(e);
                        subscriber.unsubscribe();
                    } catch (StreamFinishedException e) {
                        subscriber.onError(e);
                        subscriber.unsubscribe();
                    } catch (InvalidCommandResponseException e) {
                        LOGGER.warn(e.getMessage(), e);
                    } catch (NoDataReceivedException e) {
                        LOGGER.warn(e.getMessage(), e);
                    } catch (UnmatchedResponseException e) {
                        LOGGER.warn(e.getMessage(), e);
                    } catch (AdapterSearchingException e) {
                        LOGGER.warn(e.getMessage(), e);
                    }
                }
            }
        });

        return observable;
    }

    protected abstract boolean hasEstablishedConnection();


    /**
     * an implementation can provide a quirk for response parsing/filtering
     *
     * @return a quirk that filters a line of raw data
     */
    protected abstract ResponseQuirkWorkaround getQuirk();

    protected Observable<DataResponse> createDataObservable() {

        Observable<DataResponse> dataObservable = Observable.create(new Observable.OnSubscribe<DataResponse>() {
            @Override
            public void call(Subscriber<? super DataResponse> subscriber) {

                while (!subscriber.isUnsubscribed()) {
                    /**
                     * poll the next possible command
                     */
                    BasicCommand cmd = pollNextCommand();
                    if (cmd != null) {
                        try {
                            commandExecutor.execute(cmd);
                        } catch (IOException e) {
                            subscriber.onError(e);
                            subscriber.unsubscribe();
                        }
                    }

                    /**
                     * read the inputstream byte by byte
                     */
                    try {
                        byte[] bytes = commandExecutor.retrieveLatestResponse();

                        try {
                            DataResponse result = processResponse(bytes);

                            /**
                             * call our subscriber!
                             */
                            if (result != null) {
                                subscriber.onNext(result);
                            }
                        } catch (AdapterSearchingException e) {
                            LOGGER.warn("Adapter still searching: " + e.getMessage());
                        } catch (NoDataReceivedException e) {
                            LOGGER.warn("No data received: " + e.getMessage());
                        } catch (InvalidCommandResponseException e) {
                            LOGGER.warn("InvalidCommandResponseException: " + e.getMessage());
                        } catch (UnmatchedResponseException e) {
                            LOGGER.warn("Unmatched response: " + e.getMessage());
                        }

                    } catch (IOException e) {
                        /**
                         * IOException signals broken connection,
                         * notify subscriber accordingly
                         */
                        subscriber.onError(e);
                        return;
                    } catch (StreamFinishedException e) {
                        /**
                         * the stream has ended, notify the subscriber
                         */
                        LOGGER.info("The stream was closed: "+e.getMessage());
                        subscriber.onCompleted();
                        return;
                    }
                }

                subscriber.onCompleted();
            }
        }).timeout(DEFAULT_NO_DATA_TIMEOUT, TimeUnit.MILLISECONDS);

        return dataObservable;
    }

    @Override
    public Observable<DataResponse> observe() {
        return createDataObservable();
    }

    /**
     * an async adapter might want to send commands
     * out irregularly or on startup. An implementation
     * can provided a command that should be executed using
     * this method.
     *
     * The returned command gets executed after a valid line of
     * response has been read.
     *
     * @return the command or null if there is no pending
     */
    protected abstract BasicCommand pollNextCommand();


    /**
     * Parse a line of response
     *
     * @param bytes the bytes
     * @return a command instace
     * @throws InvalidCommandResponseException
     * @throws NoDataReceivedException
     * @throws UnmatchedResponseException
     * @throws AdapterSearchingException
     */
    protected abstract DataResponse processResponse(byte[] bytes) throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException;

}
