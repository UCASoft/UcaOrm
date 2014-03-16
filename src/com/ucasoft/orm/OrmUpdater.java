package com.ucasoft.orm;

import com.ucasoft.orm.exceptions.NotFindTableAnnotation;
import com.ucasoft.orm.exceptions.WrongListReference;
import com.ucasoft.orm.exceptions.WrongRightJoinReference;

/**
 * Created by UCASoft.
 * User: Antonov Sergey
 * Date: 16.03.14
 * Time: 13:00
 */
public class OrmUpdater {

    private Class<? extends OrmEntity> entityClass;

    public OrmUpdater(Class<? extends OrmEntity> entityClass) {
        this.entityClass = entityClass;
    }

    public void addColumn(String columnName) throws NotFindTableAnnotation, WrongRightJoinReference, WrongListReference {
        OrmField field = OrmFieldWorker.findAnnotationField(entityClass, columnName);
        String sql = String.format("%s ADD COLUMN %s %s;", getAlterTable(), columnName, DbColumn.getColumnType(field));
        OrmFactory.getDatabase().execSQL(sql);
    }

    private String getAlterTable() throws NotFindTableAnnotation {
        return String.format("ALTER TABLE %s", OrmTableWorker.getTableName(entityClass));
    }
}
