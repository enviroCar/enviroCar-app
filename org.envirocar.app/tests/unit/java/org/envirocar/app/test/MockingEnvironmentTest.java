/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.test;

import android.os.Environment;
import android.util.Base64;

import org.envirocar.app.logging.Handler;
import org.envirocar.core.logging.Logger;
import org.junit.Before;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.File;
import java.io.IOException;

/**
 * Created by matthes on 27.08.15.
 */
public class MockingEnvironmentTest {

    @Before
    public void setup() throws Exception {
        mockEnvironment();

        mockLogger();

        mockBase64();
    }

    private void mockBase64() {
        PowerMockito.mockStatic(Base64.class);
        PowerMockito.when(Base64.encodeToString(Matchers.any(), Matchers.anyInt())).thenReturn("mock");
    }

    private void mockLogger() throws Exception {
        PowerMockito.mockStatic(Logger.class);
        PowerMockito.when(Logger.getLocalFileHandler()).thenReturn(new Handler() {
            @Override
            public void logMessage(int level, String string) {

            }

            @Override
            public void initializeComplete() {

            }
        });
        PowerMockito.when(Logger.getLogger(Matchers.anyString())).thenCallRealMethod();
        PowerMockito.when(Logger.getLogger(Matchers.any(Class.class))).thenCallRealMethod();

        Logger logger = Mockito.mock(Logger.class);
        Mockito.doNothing().when(logger).info(Matchers.anyString());
        PowerMockito.whenNew(Logger.class).withAnyArguments().thenReturn(logger);
    }

    private void mockEnvironment() {
        PowerMockito.mockStatic(Environment.class);
        PowerMockito.when(Environment.getExternalStorageDirectory()).thenReturn(new File(getClass().getResource("/").getFile()));
    }
}
