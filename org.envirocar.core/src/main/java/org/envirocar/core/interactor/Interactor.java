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
package org.envirocar.core.interactor;

import com.google.common.base.Preconditions;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;

/**
 * @author dewall
 */
public abstract class Interactor<Result, Parameters> {

    private final Scheduler observeOn;
    private final Scheduler subscribeOn;
    private final CompositeDisposable disposables;

    /**
     * Cosntructor.
     *
     * @param observeOn the thread to observe on.
     * @param subscribeOn the thread to subscribe on.
     */
    public Interactor(Scheduler observeOn, Scheduler subscribeOn) {
        this.disposables = new CompositeDisposable();
        this.observeOn = observeOn;
        this.subscribeOn = subscribeOn;
    }

    public void execute(DisposableObserver<Result> observer, Parameters parameters){
        Preconditions.checkNotNull(observer);
        this.disposables.add(execute(parameters).subscribeWith(observer));
    }

    public Observable<Result> execute(Parameters parameters){
//        Preconditions.checkNotNull(parameters);
        return buildObservable(parameters)
                .subscribeOn(subscribeOn)
                .observeOn(observeOn);
    }

    public void dispose(){
        if(!disposables.isDisposed()){
            disposables.dispose();
        }
    }

    protected abstract Observable<Result> buildObservable(Parameters parameters);
}
