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
