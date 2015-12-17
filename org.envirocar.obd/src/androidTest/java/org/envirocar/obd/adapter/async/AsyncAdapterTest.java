package org.envirocar.obd.adapter.async;

import android.test.InstrumentationTestCase;

import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.entity.SpeedResponse;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.InvalidCommandResponseException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.UnmatchedResponseException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class AsyncAdapterTest extends InstrumentationTestCase {

    @Test
    public void testWorkflow() {
        MockAdapter adapter = new MockAdapter();

        InputStream is = new ByteArrayInputStream("DUMMY>DUMMY>".getBytes());
        OutputStream os = new ByteArrayOutputStream();

        TestSubscriber<Boolean> initSub = new TestSubscriber<>();

        adapter.initialize(is, os).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(initSub);

        initSub.assertNoErrors();
        initSub.assertValueCount(1);

        TestSubscriber<DataResponse> dataSub = new TestSubscriber<>();

        adapter.observe().subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(dataSub);

        List<DataResponse> list = dataSub.getOnNextEvents();
        dataSub.assertNoErrors();
        Assert.assertThat(list.size(), CoreMatchers.is(1));
        Assert.assertThat(list.get(0), CoreMatchers.is(CoreMatchers.instanceOf(SpeedResponse.class)));
    }

    private static class MockAdapter extends AsyncAdapter {

        private static final char CARRIAGE_RETURN = '\r';
        private static final char END_OF_LINE_RESPONSE = '>';
        private final Queue<BasicCommand> commands;

        public MockAdapter() {
            super(CARRIAGE_RETURN, END_OF_LINE_RESPONSE);
            this.commands = new ArrayDeque<>();

            this.commands.offer(new CarriageReturnCommand());
            this.commands.offer(new CycleCommand(Collections.singletonList(CycleCommand.DriveDeckPID.SPEED)));
        }

        @Override
        protected BasicCommand pollNextCommand() {
            return this.commands.poll();
        }

        @Override
        protected DataResponse processResponse(byte[] bytes) throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
            if (new String(bytes).equals("DUMMY")) {
                return new SpeedResponse(123);
            }
            return null;
        }

        @Override
        public boolean supportsDevice(String deviceName) {
            return true;
        }

        @Override
        public boolean hasVerifiedConnection() {
            return true;
        }
    }
}
