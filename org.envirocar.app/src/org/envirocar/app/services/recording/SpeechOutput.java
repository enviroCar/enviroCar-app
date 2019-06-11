package org.envirocar.app.services.recording;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import com.squareup.otto.Subscribe;

import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.core.events.gps.GpsSatelliteFix;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.logging.Logger;

import java.util.Locale;

import rx.Subscription;

/**
 * Handles the text to speech output of the enviroCar app.
 */
public class SpeechOutput {
    private static final Logger LOG = Logger.getLogger(SpeechOutput.class);


    // text to speech variables
    private boolean ttsAvailable = false;
    private TextToSpeech tts;

    // preference subscription
    private boolean ttsEnabled = false;
    private Subscription ttsPrefSubscription;

    // gps satellite events
    private GpsSatelliteFix latestFix;

    /**
     * Constructor.
     *
     * @param context
     */
    public SpeechOutput(Context context) {
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

        // subscription that handles preference changes
        ttsPrefSubscription =
                PreferencesHandler.getTextToSpeechObservable(context)
                        .subscribe(aBoolean -> ttsEnabled = aBoolean);
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

    protected void onCreate(Context context) {
        // subscription that handles preference changes
        ttsPrefSubscription =
                PreferencesHandler.getTextToSpeechObservable(context)
                        .subscribe(aBoolean -> ttsEnabled = aBoolean);
    }

    /**
     * Needs to be called when the recording has been stopped.
     */
    protected void onDestroy() {
        if (ttsPrefSubscription != null && !ttsPrefSubscription.isUnsubscribed()) {
            ttsPrefSubscription.unsubscribe();
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
