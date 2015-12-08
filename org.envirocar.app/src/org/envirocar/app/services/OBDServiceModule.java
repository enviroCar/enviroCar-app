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
package org.envirocar.app.services;

import android.content.Context;
import android.os.PowerManager;

import com.squareup.otto.Bus;

import org.envirocar.app.events.TrackDetailsProvider;
import org.envirocar.core.injection.InjectApplicationScope;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author dewall
 */
@Module(
        complete = false,
        library = true,
        injects = {OBDConnectionService.class}
)
public class OBDServiceModule {

//    @Singleton
//    @Provides
//    TextToSpeech provideTextToSpeech(){
//        return new TextToSpeech()
//    }

    @Singleton
    @Provides
    PowerManager.WakeLock provideWakeLock(@InjectApplicationScope Context context){
        return ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakelock");
    }

    @Singleton
    @Provides
    TrackDetailsProvider provideTrackDetails(Bus bus){
        return new TrackDetailsProvider(bus);
    }
}
