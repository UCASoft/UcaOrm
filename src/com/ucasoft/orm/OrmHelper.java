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

    public abstract void onCreate();

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
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
