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

import rx.Observable;
import rx.Subscriber;

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
        }
        else {
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
            LOGGER.log(this.currentLogLevel, "Sending bytes: "+ new String(bytes));
        }

        // write to OutputStream, or in this case a BluetoothSocket
        synchronized (this) {
            outputStream.write(bytes);
            outputStream.write(endOfLineOutput);
            outputStream.flush();
        }
    }

    public Observable<byte[]> createRawByteObservable() {
        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(Subscriber<? super byte[]> subscriber) {
                try {
                    while (!subscriber.isUnsubscribed()) {
                        byte[] bytes = readResponseLine();
                        subscriber.onNext(bytes);
                    }
                } catch (IOException e) {
                    subscriber.onError(e);
                } catch (StreamFinishedException e) {
                    subscriber.onCompleted();
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
            LOGGER.info("Detected quirk: "+this.quirk.getClass().getSimpleName());

            //re-add the end of line, it was dismissed previously
            baos.write(this.endOfLineInput);
            readUntilLineEnd(baos);
            byteArray = baos.toByteArray();
        }

        if (byteArray.length == 0){
            LOGGER.info("Unexpected empty line anomaly detected. Try to read next line.");
            baos.reset();
            readUntilLineEnd(baos);
            byteArray = baos.toByteArray();
        }

        if (LOGGER.isEnabled(currentLogLevel)) {
            LOGGER.log(currentLogLevel, "Received bytes: "+ Base64.encodeToString(byteArray, Base64.DEFAULT));
        }

        return byteArray;
    }

    private void readUntilLineEnd(ByteArrayOutputStream baos) throws IOException, StreamFinishedException {
        int i = inputStream.read();
        byte b = (byte) i;
        while (b != this.endOfLineInput) {
            if (i == -1) {
                throw new StreamFinishedException("Stream finished");
            }

            if (!ignoredChars.contains(b)){
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
