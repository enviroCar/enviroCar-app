package org.envirocar.obd.adapter.async;

import org.envirocar.obd.adapter.OBDAdapter;
import org.envirocar.obd.commands.response.DataResponse;

import java.io.InputStream;
import java.io.OutputStream;

import rx.Observable;

/**
 * Created by matthes on 03.11.15.
 */
public abstract class AsyncAdapter implements OBDAdapter {

    @Override
    public Observable<Boolean> initialize(InputStream is, OutputStream os) {
        return null;
    }

    @Override
    public Observable<DataResponse> observe() {
        return null;
    }


}
