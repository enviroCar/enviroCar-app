package org.envirocar.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.envirocar.core.injection.InjectionActivityScope;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class EnviroCarDBOpenHelper extends SQLiteOpenHelper {
    private static final Logger LOG = Logger.getLogger(EnviroCarDBOpenHelper.class);

    public static final String DATABASE_NAME = "obd2";
    public static final int DATABASE_VERSION = 9;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    @Inject
    public EnviroCarDBOpenHelper(@InjectionActivityScope Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            //db.execSQL("PRAGMA foreign_keys=ON;");
            db.execSQL(TrackTable.CREATE);
            db.execSQL(MeasurementTable.CREATE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOG.info(String.format("Updating database from version %s to version %s.", oldVersion,
                newVersion));
        db.beginTransaction();
        try {
            db.execSQL(MeasurementTable.DELETE);
            db.execSQL(TrackTable.CREATE);
        } finally {
            db.endTransaction();
        }
        onCreate(db);
    }
}
