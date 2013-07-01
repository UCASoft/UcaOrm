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

    public static OrmHelper getHelper() {
        return ormHelper;
    }

    public static SQLiteDatabase getDatabase() {
        return database;
    }

    public static void SetHelper(Class<? extends OrmHelper> ormHelperClass, Context context) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Constructor<?> constructor = ormHelperClass.getDeclaredConstructor(Context.class, String.class, SQLiteDatabase.CursorFactory.class, int.class);
        ormHelper = (OrmHelper) constructor.newInstance(context, "Database.db", null, 1);
        ormHelper.getWritableDatabase();
    }

    public static void ReleaseHelper() {
        database.close();
        ormHelper = null;
    }

    public static void setDatabase(SQLiteDatabase database) {
        OrmFactory.database = database;
    }
}
