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
