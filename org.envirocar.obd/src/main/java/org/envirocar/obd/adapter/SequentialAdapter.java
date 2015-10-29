package org.envirocar.obd.adapter;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.obd.commands.EngineLoad;
import org.envirocar.obd.commands.IntakePressure;
import org.envirocar.obd.commands.IntakeTemperature;
import org.envirocar.obd.commands.MAF;
import org.envirocar.obd.commands.O2LambdaProbe;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.commands.RPM;
import org.envirocar.obd.commands.Speed;
import org.envirocar.obd.commands.TPS;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.ResponseParser;
import org.envirocar.obd.protocol.OBDConnector;
import org.envirocar.obd.protocol.exception.AdapterFailedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private Set<Character> ignoredChars = new HashSet<>(Arrays.asList(COMMAND_RECEIVE_SPACE, COMMAND_SEND_END));
    private CommandExecutor commandExecutor;
    private ResponseParser parser = new ResponseParser();

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
                                DataResponse response = parser.parse(bytes);
                                subscriber.onNext(response);
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

    protected List<CommonCommand> defaultCycleCommands() {
        List<CommonCommand> requestCommands = new ArrayList<>();

        requestCommands.add(new Speed());
        requestCommands.add(new MAF());
        requestCommands.add(new RPM());
        requestCommands.add(new IntakePressure());
        requestCommands.add(new IntakeTemperature());
        requestCommands.add(new EngineLoad());
        requestCommands.add(new TPS());
        requestCommands.add(O2LambdaProbe.fromPIDEnum(PIDUtil.PID.O2_LAMBDA_PROBE_1_VOLTAGE));
        requestCommands.add(O2LambdaProbe.fromPIDEnum(PIDUtil.PID.O2_LAMBDA_PROBE_1_CURRENT));

        return Collections.unmodifiableList(requestCommands);
    }

    private List<CommonCommand> preparePendingCommands() {
        return Collections.unmodifiableList(providePendingCommands());
    }

    protected abstract List<CommonCommand> providePendingCommands();

    /**
     *
     * @param response the raw data received
     * @param sentCommand the originating command
     * @return true if the adapter established a meaningful connection
     */
    protected abstract boolean analyzeMetadataResponse(byte[] response, CommonCommand sentCommand) throws AdapterFailedException;

    public abstract boolean supportsDevice(String deviceName);
}
