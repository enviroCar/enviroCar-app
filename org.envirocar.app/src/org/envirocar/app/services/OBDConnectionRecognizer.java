package org.envirocar.app.services;

import android.content.Context;

import com.squareup.otto.Subscribe;

import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.SpeedUpdateEvent;

import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */

