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

import android.test.InstrumentationTestCase;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.request.PIDCommand;
import org.envirocar.obd.commands.request.elm.ConfigurationCommand;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.entity.MAFResponse;
import org.envirocar.obd.commands.response.entity.SpeedResponse;
import org.envirocar.obd.exception.AdapterFailedException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class SyncAdapterTest extends InstrumentationTestCase {

    @Test
    public void testInit() throws InterruptedException {
        MockAdapter adapter = new MockAdapter();

        ByteArrayInputStream bis = new ByteArrayInputStream("OK>OK>4100BE1FA813>1A090F01>".getBytes());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();

        Subscription sub = adapter.initialize(bis, bos)
                .observeOn(Schedulers.immediate())
                .subscribeOn(Schedulers.immediate())
                .subscribe(testSubscriber);

        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);
    }

    @Test
    public void testData() {
        MockAdapter adapter = new MockAdapter();

        //Meta, Meta, PIDSupported0x00, PIDSupported0x20, data, data
        ByteArrayInputStream bis = new ByteArrayInputStream("OK>OK>4100BE1FA813>41201A090F01>4110aabb>410daabb>".getBytes());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        TestSubscriber<Boolean> initSubscriber = new TestSubscriber<>();

        adapter.initialize(bis, bos)
                .subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(initSubscriber);

        initSubscriber.assertNoErrors();
        initSubscriber.assertValueCount(1);

        /**
         * now the actual data stuff
         */
        TestSubscriber<DataResponse> testSubscriber = new TestSubscriber<>();

        adapter.observe()
                .observeOn(Schedulers.immediate())
                .subscribeOn(Schedulers.immediate())
                .subscribe(testSubscriber);

        testSubscriber.assertNoErrors();

        List<DataResponse> received = testSubscriber.getOnNextEvents();

        Assert.assertThat(received.size(), CoreMatchers.is(2));

        Assert.assertThat(received.get(0), CoreMatchers.is(CoreMatchers.instanceOf(MAFResponse.class)));
        Assert.assertThat(received.get(1), CoreMatchers.is(CoreMatchers.instanceOf(SpeedResponse.class)));
    }

    private static class MockAdapter extends SyncAdapter {

        private final Queue<BasicCommand> initCommands;
        private final Queue<PIDCommand> dataCommands;
        private int metaResponse;

        public MockAdapter() {
            this.initCommands = new ArrayDeque<>();

            this.initCommands.offer(ConfigurationCommand.instance(ConfigurationCommand.Instance.ECHO_OFF));
            this.initCommands.offer(ConfigurationCommand.instance(ConfigurationCommand.Instance.HEADERS_OFF));

            this.dataCommands = new ArrayDeque<>();
            this.dataCommands.offer(PIDUtil.instantiateCommand(PID.MAF));
            this.dataCommands.offer(PIDUtil.instantiateCommand(PID.SPEED));
        }

        public int getMetaResponse() {
            return metaResponse;
        }

        @Override
        protected BasicCommand pollNextInitializationCommand() {
            return this.initCommands.poll();
        }

        @Override
        protected List<PIDCommand> providePendingCommands() {
            return new ArrayList<>(this.dataCommands);
        }

        @Override
        protected PIDCommand pollNextCommand() throws AdapterFailedException {
            return this.dataCommands.poll();
        }

        @Override
        protected boolean analyzeMetadataResponse(byte[] response, BasicCommand sentCommand) throws AdapterFailedException {
            return ++metaResponse >= 2;
        }

        @Override
        protected byte[] preProcess(byte[] bytes) {
            return bytes;
        }

        @Override
        public boolean supportsDevice(String deviceName) {
            return true;
        }

        @Override
        public boolean hasCertifiedConnection() {
            return true;
        }
    }
}
