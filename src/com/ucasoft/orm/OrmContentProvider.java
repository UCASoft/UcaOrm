package com.ucasoft.orm;

import android.content.ContentProvider;
import com.ucasoft.orm.exceptions.IllegalUpdateException;
import com.ucasoft.orm.exceptions.NotFindTableAnnotation;
import com.ucasoft.orm.exceptions.WrongListReference;
import com.ucasoft.orm.exceptions.WrongRightJoinReference;

import java.util.List;

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
            if (where == null) {
                Class<T> clazz = (Class<T>) OrmUtils.getTypeArguments(OrmContentProvider.class, this.getClass()).get(0);
                select = T.getAllEntities(clazz);
            } else
                select = where.Select();
            return new OrmCursor<T>(select);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int update(T entity) throws IllegalUpdateException {
        Class<? extends OrmEntity> joinLeftClass = null;
        OrmField primaryKeyField;
        try {
            joinLeftClass = OrmTableWorker.getTableJoinLeftClass(entity.getClass());
            if (joinLeftClass != null)
                primaryKeyField = OrmFieldWorker.getPrimaryKeyField(joinLeftClass);
            else
                primaryKeyField = OrmFieldWorker.getPrimaryKeyField(entity.getClass());
            primaryKeyField.setAccessible(true);
            if (primaryKeyField.get(entity) != null) {
                throw new IllegalUpdateException();
            }
            return entity.alter() ? 1 : 0;
        } catch (NotFindTableAnnotation notFindTableAnnotation) {
            notFindTableAnnotation.printStackTrace();
        } catch (WrongRightJoinReference wrongRightJoinReference) {
            wrongRightJoinReference.printStackTrace();
        } catch (WrongListReference wrongListReference) {
            wrongListReference.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
