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
package org.envirocar.app.injection;


import android.content.Context;
import androidx.fragment.app.Fragment;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.BaseApplicationComponent;

import java.lang.reflect.Field;

import javax.inject.Inject;


/**
 * @author dewall
 */
public abstract class
BaseInjectorFragment extends Fragment {
    private static final String TAG = BaseInjectorFragment.class.getSimpleName();

//    private static final Field mChildFragmentManagerFieldOfFragment;

//    static {
//        Field f = null;
//        try {
//            f = Fragment.class.getDeclaredField("mChildFragmentManager");
//            f.setAccessible(true);
//        } catch (NoSuchFieldException e) {
//            Log.e(TAG, "Error while getting the declared mChildFragmentManager field", e);
//        }
//        mChildFragmentManagerFieldOfFragment = f;
//    }

    /**
     * The event bus allows publish-subscribe-style communication. It dispatches
     * events to subscribed listeners, and provides ways for listeners to
     * register themselves.
     */
    @Inject
    protected Bus mBus;

    /**
     * Flag that indicates that the fragment is already attached and the object
     * graph was initialized.
     */
    private boolean mAlreadyAttached;
    private boolean mIsRegistered;

    protected abstract void injectDependencies(BaseApplicationComponent baseApplicationComponent);

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Check if it is the first time where this fragment was attached to the
        // activity. If false, the ObjectGraph will be extended by fragment
        // specific modules and the dependencies will be injected.
        if (!mAlreadyAttached) {

            injectDependencies(BaseApplication.get(context).getBaseApplicationComponent());

            mAlreadyAttached = true;

            Preconditions.checkState(mBus != null, "Bus has to be injected before "
                    + "registering the providers and subscribers.");
        }

        if(!mIsRegistered){
            // Register ourselves on the bus
            mBus.register(this);
            mIsRegistered = true;
        }
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach() fragment");
        super.onDetach();
//        try {
//            mChildFragmentManagerFieldOfFragment.set(this, null);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }

        if(mIsRegistered) {
            mBus.unregister(this);
            mIsRegistered = false;
        }
    }

    protected void runAfterInflation(Runnable runnable){
        getActivity().getWindow().getDecorView().post(runnable);
    }
}
