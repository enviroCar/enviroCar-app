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
package org.envirocar.app.recording.notification;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.hwangjr.rxbus.Bus;
import com.hwangjr.rxbus.annotation.Subscribe;

import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.core.events.gps.GpsSatelliteFix;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.logging.Logger;

import java.util.Locale;

import io.reactivex.disposables.Disposable;


/**
 * Handles the text to speech output of the enviroCar app.
 *
 * @author dewall
 */
public class SpeechOutput implements LifecycleObserver {
    private static final Logger LOG = Logger.getLogger(SpeechOutput.class);

    private final Bus eventBus;
    private final Context context;

    // text to speech variables
    private boolean ttsAvailable = false;
    private TextToSpeech tts;

    // preference subscription
    private boolean ttsEnabled = false;
    private Disposable ttsPrefSubscription;

    // gps satellite events
    private GpsSatelliteFix latestFix;

    /**
     * Constructor.
     *
     * @param context
     */
    public SpeechOutput(Context context, Bus eventBus) {
        this.context = context;
        this.eventBus = eventBus;

        // init
        this.tts = new TextToSpeech(context, status -> {
            try {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.ENGLISH);
                    ttsAvailable = true;
                } else {
                    LOG.warn("TextToSpeech is not available.");
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("TextToSpeech is not available");
            }
        });
    }

    /**
     * Performs a text to speech.
     *
     * @param text the text to speech.
     */
    public void doTextToSpeech(String text) {
        if (this.ttsEnabled && ttsAvailable) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
        }
    }

    /**
     * Lifecycle Event hook. Should be called when the onCreate method of the Recording Service was called.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    protected void onCreate() {
        // subscription that handles preference changes
        ttsPrefSubscription =
                ApplicationSettings.getTextToSpeechObservable(this.context)
                        .subscribe(aBoolean -> ttsEnabled = aBoolean);

        LOG.info("Registering on eventBus");
        this.eventBus.register(this);
    }

    /**
     * Needs to be called when the recording service has been stopped.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    protected void onDestroy() {
        // unsubscribe
        if (ttsPrefSubscription != null && !ttsPrefSubscription.isDisposed()) {
            ttsPrefSubscription.dispose();
        }

        // try to unregister from event bus.
        try {
            this.eventBus.unregister(this);
        } catch (IllegalArgumentException e){
            // nothing
        }
    }

    /**
     * Subscribes for GPS Satellite fix events.
     *
     * @param event
     */
    @Subscribe
    public void onReceiveSatelliteFixEvent(GpsSatelliteFixEvent event) {
        boolean isFix = event.mGpsSatelliteFix.isFix();
        if (latestFix != null && isFix != latestFix.isFix()) {
            if (isFix) {
                if (isFix) {
                    doTextToSpeech("GPS positioning established");
                } else {
                    doTextToSpeech("GPS positioning lost. Try to move the phone");
                }
                this.latestFix = event.mGpsSatelliteFix;
            }
        }
    }
}
