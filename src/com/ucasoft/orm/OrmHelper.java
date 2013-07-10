package com.ucasoft.orm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * Date: 22.01.13
 * Time: 21:56
 */
public abstract class OrmHelper extends SQLiteOpenHelper implements OrmUtils.DefaultValues {

    protected abstract void onCreate();
    protected abstract void onUpgrade(int oldVersion, int newVersion);

    public OrmHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        OrmFactory.setDatabase(database);
        onCreate();
    }

    @Override
    public void onOpen(SQLiteDatabase database) {
        super.onOpen(database);
        OrmFactory.setDatabase(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        OrmFactory.setDatabase(database);
        onUpgrade(oldVersion, newVersion);
    }
}
