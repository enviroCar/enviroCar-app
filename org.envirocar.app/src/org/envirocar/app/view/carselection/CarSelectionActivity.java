package org.envirocar.app.view.carselection;

import android.os.AsyncTask;

import org.envirocar.app.BaseMainActivity;
import org.envirocar.app.injection.BaseInjectorActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dewall
 */
public class CarSelectionActivity extends BaseInjectorActivity{




    @Override
    public List<Object> getInjectionModules() {
        return new ArrayList<>();
    }

    private class SensorRetrievalTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}
