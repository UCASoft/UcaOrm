package com.ucasoft.orm;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by UCASoft.
 * User: Antonov Sergey
 * Date: 09.12.2014.
 * Time: 14:13.
 */
public class OrmTransaction {

    public interface Worker {
        boolean work();
    }

    public static void WorkInTransaction(Worker worker) {
        if (worker != null) {
            SQLiteDatabase database = OrmFactory.getDatabase();
            database.beginTransaction();
            if (worker.work())
                database.setTransactionSuccessful();
            database.endTransaction();
        }
    }
}
