package com.ucasoft.orm;

import com.ucasoft.orm.exceptions.NotFindPrimaryKeyField;
import com.ucasoft.orm.exceptions.NotFindTableAnnotation;
import com.ucasoft.orm.exceptions.WrongListReference;
import com.ucasoft.orm.exceptions.WrongRightJoinReference;

import java.util.ArrayList;
import java.util.List;

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

    public void addColumn(String columnName) throws NotFindPrimaryKeyField, WrongRightJoinReference, WrongListReference, NotFindTableAnnotation {
        addColumn(columnName, null);
    }

    public void addColumn(String columnName, Object defaultValue) throws NotFindTableAnnotation, WrongRightJoinReference, WrongListReference, NotFindPrimaryKeyField {
        OrmField field = OrmFieldWorker.findAnnotationField(entityClass, columnName);
        addColumn(new DbColumn(field), defaultValue);
    }

    private void addColumn(DbColumn column, Object defaultValue) throws NotFindTableAnnotation {
        String sql = String.format("%s ADD COLUMN %s %s", getAlterTable(), column.getName(), column.getType());
        if (defaultValue != null) {
            sql += " DEFAULT " + defaultValue.toString();
        }
        sql += ";";
        OrmFactory.getDatabase().execSQL(sql);
    }

    public void removeColumn(String columnName) throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference, NotFindPrimaryKeyField {
        OrmField field = OrmFieldWorker.findAnnotationField(entityClass, columnName);
        removeColumn(new DbColumn(field));
    }

    private void removeColumn(DbColumn column) throws NotFindTableAnnotation, WrongRightJoinReference, WrongListReference, NotFindPrimaryKeyField {
        List<DbColumn> baseColumns = OrmUtils.getBaseColumns(entityClass);
        String columns_to = "";
        String columns_from = "";
        for (DbColumn baseColumn : baseColumns) {
            if (!baseColumn.getName().equals(column.getName())) {
                columns_to += ", " + new DbColumn(OrmFieldWorker.findAnnotationField(entityClass, baseColumn.getName()));
                columns_from += ", " + baseColumn.getName();
            }
        }
        if (!columns_to.equals("")) {
            columns_to = columns_to.substring(2);
            columns_from = columns_from.substring(2);
        }
        int random = Long.valueOf(Math.round(1 + Math.random() * 99)).intValue();
        String tableName = OrmTableWorker.getTableName(entityClass);
        String new_table_name = String.format("%s_%s", tableName, random);
        String sql = String.format("BEGIN TRANSACTION; CREATE TABLE %1$s(%2$s); INSERT INTO %1$s SELECT %3$s FROM %4$s; DROP TABLE %4$s; ALTER TABLE %1$s RENAME TO %4$s; COMMIT;", new_table_name, columns_to, columns_from, tableName);
        OrmFactory.getDatabase().execSQL(sql);
    }

    public void renameColumn(String oldColumnName, String newColumnName) throws NotFindTableAnnotation, WrongRightJoinReference, WrongListReference, NotFindPrimaryKeyField {
        List<DbColumn> baseColumns = OrmUtils.getBaseColumns(entityClass);
        String columns_to = "";
        String columns_from = "";
        for (DbColumn baseColumn : baseColumns) {
            OrmField field;
            if (baseColumn.getName().equals(oldColumnName)) {
                field = OrmFieldWorker.findAnnotationField(entityClass, newColumnName);
                columns_to += ", " + new DbColumn(newColumnName, DbColumn.getColumnType(field), DbColumn.getColumnAdditional(field));
            } else {
                field = OrmFieldWorker.findAnnotationField(entityClass, baseColumn.getName());
                columns_to += ", " + new DbColumn(field);
            }
            columns_from += ", " + baseColumn.getName();
        }
        if (!columns_to.equals("")) {
            columns_to = columns_to.substring(2);
            columns_from = columns_from.substring(2);
        }
        int random = Long.valueOf(Math.round(1 + Math.random() * 99)).intValue();
        String tableName = OrmTableWorker.getTableName(entityClass);
        String new_table_name = String.format("%s_%s", tableName, random);
        String sql = String.format("BEGIN TRANSACTION; CREATE TABLE %1$s(%2$s); INSERT INTO %1$s SELECT %3$s FROM %4$s; DROP TABLE %4$s; ALTER TABLE %1$s RENAME TO %4$s; COMMIT;", new_table_name, columns_to, columns_from, tableName);
        OrmFactory.getDatabase().execSQL(sql);
    }

    private String getAlterTable() throws NotFindTableAnnotation {
        return String.format("ALTER TABLE %s", OrmTableWorker.getTableName(entityClass));
    }

    public void work() throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongListReference, WrongRightJoinReference {
        List<DbColumn> baseColumns = OrmUtils.getBaseColumns(entityClass);
        List<DbColumn> classColumns = OrmUtils.getAllColumn(entityClass);
        List<DbColumn> addColumns = new ArrayList<DbColumn>();
        List<DbColumn> removeColumns;
        List<DbColumn> changeTypeColumns = new ArrayList<DbColumn>();
        for (DbColumn classColumn :  classColumns) {
            boolean findInBase = false;
            for (int i = baseColumns.size() - 1; i >= 0; i--) {
                DbColumn baseColumn = baseColumns.get(i);
                if (baseColumn.getName().equals(classColumn.getName())) {
                    if (!baseColumn.getType().equals(classColumn.getType())) {
                        changeTypeColumns.add(classColumn);
                    }
                    baseColumns.remove(i);
                    findInBase = true;
                    break;
                }
            }
            if (!findInBase) {
                addColumns.add(classColumn);
            }
        }
        removeColumns = baseColumns;
        for (DbColumn column : addColumns) {
            addColumn(column, null);
        }
        for (DbColumn column : removeColumns) {
            removeColumn(column);
        }
    }
}
