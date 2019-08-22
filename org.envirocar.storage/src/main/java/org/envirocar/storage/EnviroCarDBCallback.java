package org.envirocar.storage;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import org.envirocar.core.logging.Logger;

/**
 * @author dewall
 */
class EnviroCarDBCallback extends SupportSQLiteOpenHelper.Callback {
    private static final Logger LOG = Logger.getLogger(EnviroCarDBOpenHelper.class);



    /**
     * Creates a new Callback to get database lifecycle events.
     *
     * @param version The version for the database instance. See {@link #version}.
     */
    public EnviroCarDBCallback(int version) {
        super(version);
    }

    @Override
    public void onCreate(SupportSQLiteDatabase db) {
        LOG.info("On create enviroCar database");
        try {
            db.execSQL(TrackTable.CREATE);
            db.execSQL(MeasurementTable.CREATE);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        LOG.info("On update enviroCar database");
        db.beginTransaction();
        try {
            db.execSQL(MeasurementTable.DELETE);
            db.execSQL(TrackTable.DELETE);
            db.execSQL(TrackTable.CREATE);
            db.execSQL(MeasurementTable.CREATE);
        } finally {
            db.endTransaction();
        }
    }
}
