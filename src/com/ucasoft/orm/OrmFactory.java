package com.ucasoft.orm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * Date: 10.02.13
 * Time: 0:39
 */
public class OrmFactory {

    private static OrmHelper ormHelper;
    private static SQLiteDatabase database;

    static OrmHelper getHelper() {
        return ormHelper;
    }

    static SQLiteDatabase getDatabase() {
        return database;
    }

    public static void SetHelper(Class<? extends OrmHelper> ormHelperClass, Context context) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Constructor<?> constructor = ormHelperClass.getDeclaredConstructor(Context.class, String.class, SQLiteDatabase.CursorFactory.class, int.class);
        ormHelper = (OrmHelper) constructor.newInstance(context, getDatabaseName(context), null, getDatabaseVersion(context));
        ormHelper.getWritableDatabase().execSQL("PRAGMA foreign_keys=ON;");
    }

    private static int getDatabaseVersion(Context context) {
        Integer version = OrmMetaData.getMetaData(context, "UO_DB_VERSION");
        if (version == null)
            version = 1;
        return version;
    }

    private static String getDatabaseName(Context context) {
        String dbName = OrmMetaData.getMetaData(context, "UO_DB_NAME");
        if (dbName == null)
            dbName = OrmMetaData.getAppName(context) + ".db";
        return dbName;
    }

    public static void ReleaseHelper() {
        database.close();
        ormHelper = null;
    }

    static void setDatabase(SQLiteDatabase database) {
        OrmFactory.database = database;
    }
}
