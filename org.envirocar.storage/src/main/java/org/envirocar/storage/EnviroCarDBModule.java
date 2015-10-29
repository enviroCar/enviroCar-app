package org.envirocar.storage;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import org.envirocar.core.injection.InjectApplicationScope;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Module(
        complete = false,
        library = true,
        injects = {
                EnviroCarDBImpl.class,
                EnviroCarDBOpenHelper.class
        }
)
public final class EnviroCarDBModule {

    @Provides
    @Singleton
    SQLiteOpenHelper provideSQLiteOpenHelper(@InjectApplicationScope Context context) {
        return new EnviroCarDBOpenHelper(context);
    }

    @Provides
    @Singleton
    SqlBrite provideSqlBrite() {
        return SqlBrite.create();
    }

    @Provides
    @Singleton
    BriteDatabase provideBriteDatabase(SqlBrite sqlBrite, SQLiteOpenHelper helper) {
        return sqlBrite.wrapDatabaseHelper(helper);
    }

    @Provides
    @Singleton
    EnviroCarDB provideEnvirocarDB(BriteDatabase briteDatabase) {
        return new EnviroCarDBImpl(briteDatabase);
    }

}
