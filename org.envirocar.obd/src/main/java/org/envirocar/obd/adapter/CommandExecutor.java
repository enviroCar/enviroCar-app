/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.obd.adapter;

import android.util.Base64;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.exception.StreamFinishedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class CommandExecutor {

    private static final Logger LOGGER = Logger.getLogger(CommandExecutor.class.getName());
    private final Set<Byte> ignoredChars;
    private final byte endOfLineOutput;
    private final byte endOfLineInput;
    private OutputStream outputStream;
    private InputStream inputStream;
    private ResponseQuirkWorkaround quirk;
    private boolean logEverything = false;
    private int currentLogLevel = Logger.DEBUG;


    public CommandExecutor(InputStream is, OutputStream os,
                           Set<Character> ignoredChars, Character endOfLineInput, Character endOfLineOutput) {
        this.inputStream = is;
        this.outputStream = os;
        this.ignoredChars = new HashSet<>(ignoredChars.size());

        for (Character c : ignoredChars) {
            this.ignoredChars.add((byte) c.charValue());
        }

        this.endOfLineOutput = (byte) endOfLineOutput.charValue();
        this.endOfLineInput = (byte) endOfLineInput.charValue();

        this.setLogEverything(false);
    }

    public final void setLogEverything(boolean logEverything) {
        this.logEverything = logEverything;
        if (this.logEverything && !LOGGER.isEnabled(Logger.DEBUG)) {
            this.currentLogLevel = Logger.INFO;
        } else {
            this.currentLogLevel = Logger.DEBUG;
        }
    }

    public void setQuirk(ResponseQuirkWorkaround quirk) {
        this.quirk = quirk;
    }

    public void execute(BasicCommand cmd) throws IOException {
        if (cmd == null) {
            throw new IOException("Command cannot be null!");
        }

        byte[] bytes = cmd.getOutputBytes();

        if (LOGGER.isEnabled(this.currentLogLevel)) {
            LOGGER.log(this.currentLogLevel, "Sending bytes: " + new String(bytes));
        }

        // write to OutputStream, or in this case a BluetoothSocket
        synchronized (this) {
            outputStream.write(bytes);
            outputStream.write(endOfLineOutput);
            outputStream.flush();
        }
    }

    public Observable<byte[]> createRawByteObservable() {
        return Observable.create(new ObservableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(ObservableEmitter<byte[]> emitter) throws Exception {
                try {
                    while (!emitter.isDisposed()) {
                        byte[] bytes = readResponseLine();
                        emitter.onNext(bytes);
                    }
                } catch (IOException e) {
                    emitter.onError(e);
                } catch (StreamFinishedException e) {
                    emitter.onComplete();
                }
            }
        });
    }


    private byte[] readResponseLine() throws IOException, StreamFinishedException {
        // TODO
//        LOGGER.info("Reading response line...");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // read until end of line arrives
        readUntilLineEnd(baos);

        byte[] byteArray = baos.toByteArray();

        //some adapter (i.e. the drivedeck) MIGHT respond with linebreaks as actual data - detect this
        if (quirk != null && quirk.shouldWaitForNextTokenLine(byteArray)) {
            LOGGER.info("Detected quirk: " + this.quirk.getClass().getSimpleName());

            //re-add the end of line, it was dismissed previously
            baos.write(this.endOfLineInput);
            readUntilLineEnd(baos);
            byteArray = baos.toByteArray();
        }

        if (byteArray.length == 0) {
            LOGGER.info("Unexpected empty line anomaly detected. Try to read next line.");
//            baos.reset();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }

            if (isDataAvailable()){
                readUntilLineEnd(baos);
                byteArray = baos.toByteArray();
            }

        }

        if (LOGGER.isEnabled(currentLogLevel)) {
            LOGGER.log(currentLogLevel, "Received bytes: " + Base64.encodeToString(byteArray, Base64.DEFAULT));
        }

        return byteArray;
    }

    public boolean isDataAvailable(){
        try {
            return inputStream.available() > 0;
        } catch (Exception e){
            return false;
        }
    }

    private void readUntilLineEnd(ByteArrayOutputStream baos) throws IOException, StreamFinishedException {
        int i = inputStream.read();
        byte b = (byte) i;
        while (b != this.endOfLineInput) {
            if (i == -1) {
                throw new StreamFinishedException("Stream finished");
            }

            if (!ignoredChars.contains(b)) {
                baos.write(b);
            }

            i = inputStream.read();
            b = (byte) i;
        }
    }

    public byte[] retrieveLatestResponse() throws IOException, StreamFinishedException {
        return readResponseLine();
    }
}
