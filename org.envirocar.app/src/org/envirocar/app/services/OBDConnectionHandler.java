/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.services;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Parcelable;

import org.envirocar.app.exception.NoOBDSocketConnectedException;
import org.envirocar.app.exception.UUIDSanityCheckFailedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.BroadcastUtils;
import org.envirocar.obd.bluetooth.BluetoothSocketWrapper;
import org.envirocar.obd.bluetooth.FallbackBluetoothSocket;
import org.envirocar.obd.bluetooth.NativeBluetoothSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class OBDConnectionHandler {
    private static final Logger LOG = Logger.getLogger(OBDConnectionHandler.class);
    private static final UUID EMBEDDED_BOARD_SPP = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Context context;

    /**
     * Constructor
     *
     * @param context The context of the current scope.
     */
    public OBDConnectionHandler(Context context) {
        this.context = context;
    }

    /**
     * @param device the device to start a connection to.
     */
    public Observable<BluetoothSocketWrapper> getOBDConnectionObservable(final BluetoothDevice device) {
        return Observable.just(device)
                .map(bluetoothDevice -> {
                    if (bluetoothDevice.fetchUuidsWithSdp())
                        return bluetoothDevice;
                    else
                        throw new UUIDSanityCheckFailedException();
                })
                .concatMap(bluetoothDevice -> getUUIDList(bluetoothDevice))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .concatMap(uuids -> createOBDBluetoothObservable(device, uuids));
    }

    public void shutdownSocket(BluetoothSocketWrapper socket) {
        LOG.info("Shutting down bluetooth socket.");

        try {
            if (socket.getInputStream() != null)
                socket.getInputStream().close();
            if (socket.getOutputStream() != null)
                socket.getOutputStream().close();
            socket.close();
        } catch (Exception e) {
            LOG.severe(e.getMessage(), e);
        }
    }

    /**
     * @param device
     * @return
     */
    private Observable<List<UUID>> getUUIDList(final BluetoothDevice device) {
        LOG.info(String.format("getUUIDList(%s)", device.getName()));

        return BroadcastUtils.createBroadcastObservable(context, new IntentFilter(BluetoothDevice.ACTION_UUID))
                .firstOrError()
                .toObservable()
                .map(intent -> {
                    LOG.info("getUUIDList(): map call");

                    // Get the device and the UUID provided by the incoming intent.
                    BluetoothDevice deviceExtra = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Parcelable[] uuidExtra = intent
                            .getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

                    // If the received broadcast does not belong to this receiver,
                    // skip it.
                    if (!deviceExtra.getAddress().equals(device.getAddress()))
                        return null;

                    // Result list to return
                    List<UUID> res = new ArrayList<UUID>();

                    LOG.info(String.format("Adding default UUID: %s", EMBEDDED_BOARD_SPP));
                    res.add(EMBEDDED_BOARD_SPP);

                    // Create a uuid for every string and return it
                    for (Parcelable uuid : uuidExtra) {
                        UUID next = UUID.fromString(uuid.toString());
                        if (!res.contains(next)) {
                            res.add(next);
                        }
                    }

                    // return the result list
                    return res;
                });
    }

    private Observable<BluetoothSocketWrapper> createOBDBluetoothObservable(
            BluetoothDevice device, List<UUID> uuids) {
        return Observable.create(new ObservableOnSubscribe<BluetoothSocketWrapper>() {
            private BluetoothSocketWrapper socketWrapper;

            @Override
            public void subscribe(ObservableEmitter<BluetoothSocketWrapper> emitter) throws Exception {
                for (UUID uuid : uuids) {
                    // Stop if the subscriber is unsubscribed.
                    if (emitter.isDisposed())
                        return;

                    try {
                        LOG.info("Trying to create native bleutooth socket");
                        socketWrapper = new NativeBluetoothSocket(device.createInsecureRfcommSocketToServiceRecord(uuid));
                    } catch (IOException e) {
                        LOG.warn(e.getMessage(), e);
                        continue;
                    }


                    try {
                        connectSocket();
                    } catch (FallbackBluetoothSocket.FallbackException |
                            InterruptedException |
                            IOException e) {
                        LOG.warn(e.getMessage(), e);
                        shutdownSocket(socketWrapper);
                        socketWrapper = null;
                    }
                    if (emitter.isDisposed()){
                        if(socketWrapper != null){
                            socketWrapper.shutdown();
                        }
                        return;
                    }

                    if (socketWrapper != null) {
                        LOG.info("successful connected");
                        emitter.onNext(socketWrapper);
                        socketWrapper = null;
                        emitter.onComplete();
                        return;
                    }
                }

                emitter.setDisposable(new Disposable() {
                    @Override
                    public void dispose() {
                        LOG.info("Disposing createOBDBluetoothObservable");
                        try {
                            if (socketWrapper != null) {
                                shutdownSocket(socketWrapper);
                                socketWrapper = null;
                            }
                        } catch (Exception e){
                            LOG.error(e);
                        }
                    }

                    @Override
                    public boolean isDisposed() {
                        return emitter.isDisposed();
                    }
                });

                if (socketWrapper == null) {
                    emitter.onError(new NoOBDSocketConnectedException());
                }
            }

            private void connectSocket() throws FallbackBluetoothSocket.FallbackException,
                    InterruptedException, IOException {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socketWrapper.connect();
                } catch (IOException e) {
                    LOG.warn("Exception on bluetooth connection. Trying the fallback... : "
                            + e.getMessage(), e);

                    //try the fallback
                    socketWrapper = new FallbackBluetoothSocket(socketWrapper.getUnderlyingSocket());
                    Thread.sleep(500);
                    socketWrapper.connect();
                }
            }

        });
    }
}
