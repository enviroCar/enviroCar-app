package org.envirocar.obd.adapter;

import android.util.Base64;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.obd.commands.exception.AdapterSearchingException;
import org.envirocar.obd.commands.exception.NoDataReceivedException;
import org.envirocar.obd.commands.exception.UnmatchedResponseException;
import org.envirocar.obd.commands.response.CommandResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by matthes on 29.10.15.
 */
public class CommandExecutor {

    private static final Logger LOGGER = Logger.getLogger(CommandExecutor.class.getName());
    private final Set<Character> ignoredChars;
    private final Character endOfLine;
    private OutputStream outputStream;
    private InputStream inputStream;

    public CommandExecutor(InputStream is, OutputStream os,
                           Set<Character> ignoredChars, Character endOfLine) {
        this.inputStream = is;
        this.outputStream = os;
        this.ignoredChars = ignoredChars;
        this.endOfLine = endOfLine;
    }

    public void execute(CommonCommand cmd) throws IOException {

        LOGGER.debug("Sending command " +cmd.getCommandName()+ " / "+ new String(cmd.getOutgoingBytes()));

        // write to OutputStream, or in this case a BluetoothSocket
        outputStream.write(cmd.getOutgoingBytes());
        outputStream.write(cmd.getEndOfLineSend());
        outputStream.flush();
    }

    public Observable<byte[]> createRawByteObservable() {
        Observable<byte[]> obs = Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(Subscriber<? super byte[]> subscriber) {
                while (subscriber.isUnsubscribed()) {
                    try {
                        byte[] bytes = readResponseLine();
                        subscriber.onNext(bytes);
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }
            }
        });

        return obs;
    }


    private byte[] readResponseLine() throws IOException {
        byte b = 0;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // read until '>' arrives
        while ((char) (b = (byte) inputStream.read()) != this.endOfLine) {
            if (!ignoredChars.contains((char) b)){
                baos.write(b);
            }
        }

        byte[] byteArray = baos.toByteArray();
        if (byteArray.length > 0) {
            LOGGER.verbose("Response read. Data (base64): "+
                    Base64.encodeToString(byteArray, Base64.DEFAULT));
        }

        return baos.toByteArray();
    }

    public byte[] retrieveLatestResponse() throws IOException {
        return readResponseLine();
    }
}
