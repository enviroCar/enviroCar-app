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
package org.envirocar.app.injection.modules;

import android.content.Context;
import android.os.PowerManager;

import org.envirocar.core.injection.InjectApplicationScope;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author dewall
 */
@Module
public class OBDServiceModule {

//    @Singleton
//    @Provides
//    PowerManager.WakeLock provideWakeLock(@InjectApplicationScope Context context) {
//        return ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
//                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "org.envirocar.app:wakelock");
//    }

//    @Singleton
//    @Provides
//    MeasurementProvider provideMeasurementProvider() {
//        return new InterpolationMeasurementProvider();
//    }

//    @Singleton
//    @Provides
//    OBDConnectionHandler provideOBDConnectionHandler(@InjectApplicationScope Context context) {
//        return new OBDConnectionHandler(context);
//    }

//    @Singleton
//    @Provides
//    SpeechOutput provideSpeechOutput(@InjectApplicationScope Context context) {
//        SpeechOutput speechOutput = new SpeechOutput(context);
//        ((BaseApplication) context).getBaseApplicationComponent().inject(speechOutput);
//        return speechOutput;
//    }

//    @Singleton
//    @Provides
//    RecordingNotification provideRecordingNotification(@InjectApplicationScope Context context) {
//        RecordingNotification recordingNotification = new RecordingNotification(context);
//        ((BaseApplication) context).getBaseApplicationComponent().inject(recordingNotification);
//        return recordingNotification;
//    }

}
