package org.envirocar.storage;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import com.squareup.sqlbrite.SqlBrite;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public final class DBModule {

    private final Context context;

    private SQLiteOpenHelper helper;
    private SqlBrite sqlBrite;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    public DBModule(final Context context) {
        this.context = context;
    }

//    @Provides
//    @Singleton
//    SQLiteOpenHelper provideSQLiteOpenHelper() {
//        if(helper == null) {
//            helper = new EnviroCarDBOpenHelper(context);
//        }
//
//        return helper;
//    }
//
//    @Provides
//    @Singleton
//    SqlBrite provideSqlBrite(){
//        if(sqlBrite == null){
//            sqlBrite = SqlBrite.create();
//        }
//        return sqlBrite;
//    }
//
//    @Provides
//    @Singleton
//    BriteDatabase provideBriteDatabase(){
//        return sqlBrite.wrapDatabaseHelper(helper);
//    }
}
