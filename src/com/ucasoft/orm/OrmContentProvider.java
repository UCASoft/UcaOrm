package com.ucasoft.orm;

import android.content.ContentProvider;
import android.database.Cursor;
import android.net.Uri;
import com.ucasoft.orm.exceptions.*;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by UCASoft.
 * User: Antonov Sergey
 * Date: 04.03.14
 * Time: 15:15
 */
public abstract class OrmContentProvider<T extends OrmEntity> extends ContentProvider {

    public OrmCursor<T> query(OrmWhere where){
        try {
            List<T> select;
            Class<T> clazz = (Class<T>) OrmUtils.getTypeArguments(OrmContentProvider.class, this.getClass()).get(0);
            if (where == null)
                select = T.getAllEntities(clazz);
            else
                select = where.Select();
            return new OrmCursor<T>(select);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }
}
