package org.envirocar.obd.adapter.async;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.adapter.CommandExecutor;
import org.envirocar.obd.adapter.OBDAdapter;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.UnmatchedResponseException;
import org.envirocar.obd.exception.InvalidCommandResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by matthes on 03.11.15.
 */
public abstract class AsyncAdapter implements OBDAdapter {

    private static Logger LOGGER = Logger.getLogger(AsyncAdapter.class);

    private static final long DEFAULT_NO_DATA_TIMEOUT = 15000;
    private final char endOfLineOutput;
    private final char endOfLineInput;
    private InputStream inputStream;
    private OutputStream outputStream;
    private byte[] globalBuffer = new byte[64];
    private int globalIndex;
    private Observable<DataResponse> dataObservable;
    private CommandExecutor commandExecutor;

    public AsyncAdapter(char endOfLineOutput, char endOfLineInput) {
        this.endOfLineOutput = endOfLineOutput;
        this.endOfLineInput = endOfLineInput;
    }

    @Override
    public Observable<Boolean> initialize(InputStream is, OutputStream os) {
        this.inputStream = is;
        this.outputStream = os;
        this.commandExecutor = new CommandExecutor(is, os, Collections.emptySet(), this.endOfLineInput, this.endOfLineOutput);

        /**
         *
         */
        Observable<Boolean> obserable = Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {

                /**
                 * use the data observable to inform about
                 * successfull connection in an asynchronous fashion:
                 * Once data has arrived (adapter automatically connected)
                 * we mark the connection as verified and call the subscriber
                 */
                dataObservable
                        .observeOn(Schedulers.io())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(new Subscriber<DataResponse>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                subscriber.onError(e);
                            }

                            @Override
                            public void onNext(DataResponse dataResponse) {
                                subscriber.onNext(Boolean.TRUE);
                            }
                        });

            }
        });

        createDataObservable();

        return obserable;
    }

    protected void createDataObservable() {
        this.dataObservable = Observable.create(new Observable.OnSubscribe<DataResponse>() {
            @Override
            public void call(Subscriber<? super DataResponse> subscriber) {
                byte byteIn;
                int intIn;

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
                        intIn = inputStream.read();
                    } catch (IOException e) {
                        /**
                         * IOException signals broken connection,
                         * notify subscriber accordingly
                         */
                        subscriber.onError(e);
                        subscriber.unsubscribe();
                        return;
                    }

                    /**
                     * is the end of the stream reached? --> break out of the loop and
                     * notify the subscriber with onCompleted
                     */
                    if (intIn < 0) {
                        break;
                    }

                    byteIn = (byte) intIn;

                    if (byteIn == (byte) endOfLineInput) {
                        /**
                         * end of line: we can parse what we got until now
                         */
                        boolean isReplete = false;

                        DataResponse result = null;
                        try {
                            result = processResponse(Arrays.copyOfRange(globalBuffer,
                                    0, globalIndex));
                        } catch (AdapterSearchingException e) {
                            LOGGER.warn("Adapter still searching: " + e.getMessage());
                        } catch (NoDataReceivedException e) {
                            LOGGER.warn("No data received: " + e.getMessage());
                        } catch (InvalidCommandResponseException e) {
                            LOGGER.warn("InvalidCommandResponseException: " + e.getMessage());
                        } catch (UnmatchedResponseException e) {
                            LOGGER.warn("Unmatched response: " + e.getMessage());
                        }

                        //reset the index in order to be able to reuse the buffer
                        globalIndex = 0;
                        subscriber.onNext(result);
                    } else {
                        /**
                         * not end of line, data byte --> add to buffer
                         */
                        globalBuffer[globalIndex++] = byteIn;
                    }

                }

                subscriber.onCompleted();
            }
        })
            .timeout(DEFAULT_NO_DATA_TIMEOUT, TimeUnit.MILLISECONDS)
                //use share() as we use the same observable at init phase
            .share();
    }

    @Override
    public Observable<DataResponse> observe() {
        return dataObservable;
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
