package org.envirocar.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.envirocar.core.logging.Logger;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class EnviroCarDBOpenHelper extends SQLiteOpenHelper {
    private static final Logger LOG = Logger.getLogger(EnviroCarDBOpenHelper.class);

    public static final String DATABASE_NAME = "";
    public static final int DATABASE_VERSION = 1;

    /**
     * @param context the context of the current scope.
     */
    public EnviroCarDBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
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
