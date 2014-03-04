package com.ucasoft.orm;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import com.ucasoft.orm.exceptions.NotFindTableAnnotation;
import com.ucasoft.orm.exceptions.WrongListReference;
import com.ucasoft.orm.exceptions.WrongRightJoinReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by UCASoft.
 * User: Antonov Sergey
 * Date: 02.03.14
 * Time: 21:04
 */
public class OrmCursor<T extends OrmEntity> implements Cursor {

    private List<T> entities;
    private int position;

    OrmCursor() {
        entities = new ArrayList<T>();
        position = -1;
    }

    OrmCursor(List<T> entities) {
        this.entities = entities;
        position = -1;
    }

    @Override
    public int getCount() {
        return entities.size();
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public boolean move(int i) {
        position += i;
        if (position < 0) {
            position = -1;
            return false;
        }
        if (position > entities.size()) {
            position = entities.size();
            return false;
        }
        return true;
    }

    @Override
    public boolean moveToPosition(int i) {
        position = i;
        return -1 <= position && position <= entities.size();
    }

    @Override
    public boolean moveToFirst() {
        position = 0;
        return entities.size() > 0;
    }

    @Override
    public boolean moveToLast() {
        position = entities.size() - 1;
        return entities.size() > 0;
    }

    @Override
    public boolean moveToNext() {
        position++;
        return position < entities.size();
    }

    @Override
    public boolean moveToPrevious() {
        position--;
        return 0 <= position;
    }

    @Override
    public boolean isFirst() {
        return position == 0;
    }

    @Override
    public boolean isLast() {
        return position == entities.size() - 1;
    }

    @Override
    public boolean isBeforeFirst() {
        return position <= -1;
    }

    @Override
    public boolean isAfterLast() {
        return entities.size() <= position;
    }

    @Override
    public int getColumnIndex(String s) {
        if (entities.size() > 0) {
            String[] columns = getColumnNames();
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].equals(s))
                    return i;
            }
        }
        return -1;
    }

    @Override
    public int getColumnIndexOrThrow(String s) throws IllegalArgumentException {
        int i = getColumnIndex(s);
        if (0 <= i)
            return i;
        throw new IllegalArgumentException();
    }

    @Override
    public String getColumnName(int i) {
        if (entities.size() > 0) {
            return getColumnNames()[i];
        }
        return null;
    }

    @Override
    public String[] getColumnNames() {
        if (entities.size() > 0) {
            try {
                List<OrmField> fields = getOrmFields();
                String[] result = new String[fields.size()];
                for (int i = 0; i < fields.size(); i++){
                    result[i] = OrmUtils.getColumnName(fields.get(i));
                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new String[0];
    }

    private List<OrmField> getOrmFields() throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference {
        return OrmFieldWorker.getAllAnnotationFields(entities.get(0).getClass());
    }

    @Override
    public int getColumnCount() {
        return getColumnNames().length;
    }

    @Override
    public byte[] getBlob(int i) {
        try {
            List<OrmField> fields = getOrmFields();
            return (byte[]) fields.get(i).get(entities.get(position));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public String getString(int i) {
        try {
            return (String) getOrmFields().get(i).get(entities.get(position));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void copyStringToBuffer(int i, CharArrayBuffer charArrayBuffer) {

    }

    @Override
    public short getShort(int i) {
        return 0;
    }

    @Override
    public int getInt(int i) {
        return 0;
    }

    @Override
    public long getLong(int i) {
        return 0;
    }

    @Override
    public float getFloat(int i) {
        return 0;
    }

    @Override
    public double getDouble(int i) {
        return 0;
    }

    @Override
    public int getType(int i) {
        return 0;
    }

    @Override
    public boolean isNull(int i) {
        return false;
    }

    @Override
    public void deactivate() {

    }

    @Override
    public boolean requery() {
        return false;
    }

    @Override
    public void close() {
        entities = null;
    }

    @Override
    public boolean isClosed() {
        return entities == null;
    }

    @Override
    public void registerContentObserver(ContentObserver contentObserver) {

    }

    @Override
    public void unregisterContentObserver(ContentObserver contentObserver) {

    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public void setNotificationUri(ContentResolver contentResolver, Uri uri) {

    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return false;
    }

    @Override
    public Bundle getExtras() {
        return null;
    }

    @Override
    public Bundle respond(Bundle bundle) {
        return null;
    }
}
