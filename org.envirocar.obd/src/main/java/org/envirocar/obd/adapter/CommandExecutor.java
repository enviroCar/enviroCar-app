package org.envirocar.obd.adapter;

import android.util.Base64;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.exception.StreamFinishedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import rx.Observable;
import rx.Subscriber;

public class CommandExecutor {

    private static final Logger LOGGER = Logger.getLogger(CommandExecutor.class.getName());
    private final Set<Character> ignoredChars;
    private final Character endOfLineOutput;
    private final char endOfLineInput;
    private OutputStream outputStream;
    private InputStream inputStream;


    public CommandExecutor(InputStream is, OutputStream os,
                           Set<Character> ignoredChars, Character endOfLineInput, Character endOfLineOutput) {
        this.inputStream = is;
        this.outputStream = os;
        this.ignoredChars = ignoredChars;
        this.endOfLineOutput = endOfLineOutput;
        this.endOfLineInput = endOfLineInput;
    }

    public void execute(BasicCommand cmd) throws IOException {
        if (cmd == null) {
            throw new IOException("Command cannot be null!");
        }

        byte[] bytes = cmd.getOutputBytes();

        if (LOGGER.isEnabled(Logger.DEBUG)) {
            LOGGER.debug("Sending bytes: "+ new String(bytes));
        }

        // write to OutputStream, or in this case a BluetoothSocket
        synchronized (this) {
            outputStream.write(bytes);
            outputStream.write(endOfLineOutput);
            outputStream.flush();
        }
    }

    public Observable<byte[]> createRawByteObservable() {
        Observable<byte[]> obs = Observable.create(new Observable.OnSubscribe<byte[]>() {
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

        return obs;
    }


    private byte[] readResponseLine() throws IOException, StreamFinishedException {
        LOGGER.info("Reading response line...");
        byte b = 0;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // read until end of line arrives
        b = (byte) inputStream.read();
        while ((char) b != this.endOfLineInput) {
            if ((int) b == -1) {
                throw new StreamFinishedException("Stream finished");
            }

            if (!ignoredChars.contains((char) b)){
                baos.write(b);
            }

            b = (byte) inputStream.read();
        }

        byte[] byteArray = baos.toByteArray();
        if (byteArray.length > 0) {
            if (LOGGER.isEnabled(LOGGER.VERBOSE)) {
                LOGGER.verbose("Response read. Data (base64): "+
                        Base64.encodeToString(byteArray, Base64.DEFAULT));
            }
        }

        return baos.toByteArray();
    }

    public byte[] retrieveLatestResponse() throws IOException, StreamFinishedException {
        return readResponseLine();
    }
}
