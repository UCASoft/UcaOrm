package com.ucasoft.orm;

import android.database.AbstractCursor;
import com.ucasoft.orm.exceptions.NotFindTableAnnotation;
import com.ucasoft.orm.exceptions.WrongListReference;
import com.ucasoft.orm.exceptions.WrongRightJoinReference;

import java.util.List;

/**
 * Created by UCASoft.
 * User: Antonov Sergey
 * Date: 02.03.14
 * Time: 21:04
 */
public class OrmCursor<T extends OrmEntity> extends AbstractCursor {

    private List<T> entities;

    public OrmCursor(List<T> entities) {
        this.entities = entities;
    }

    @Override
    public int getCount() {
        return entities.size();
    }

    public List<T> getEntities() {
        return entities;
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

    @Override
    public String getString(int column) {
        Object object = getObject(column);
        if (object != null)
            return object.toString();
        return null;
    }

    @Override
    public short getShort(int column) {
        Object object = getString(column);
        if (object != null)
            return Short.parseShort(object.toString());
        return 0;
    }

    @Override
    public int getInt(int column) {
        Object object = getString(column);
        if (object != null)
            return Integer.parseInt(object.toString());
        return 0;
    }

    @Override
    public long getLong(int column) {
        Object object = getString(column);
        if (object != null)
            return Long.parseLong(object.toString());
        return 0;
    }

    @Override
    public float getFloat(int column) {
        Object object = getString(column);
        if (object != null)
            return Float.parseFloat(object.toString());
        return 0;
    }

    @Override
    public double getDouble(int column) {
        Object object = getString(column);
        if (object != null)
            return Double.parseDouble(object.toString());
        return 0;
    }

    @Override
    public boolean isNull(int column) {
        return getObject(column) == null;
    }

    private List<OrmField> getOrmFields() throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference {
        return OrmFieldWorker.getAllAnnotationFields(entities.get(0).getClass());
    }

    private Object getObject(int column) {
        try {
            return getOrmFields().get(column).get(entities.get(getPosition()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}