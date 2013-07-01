package com.ucasoft.orm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ucasoft.orm.annotations.Column;
import com.ucasoft.orm.annotations.Table;
import com.ucasoft.orm.exceptions.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 27.06.13
 * Time: 9:26
 */
public class OrmUtils {

    public static boolean alter(OrmEntity entity) throws WrongRightJoinReference, NotFindTableAnnotation, IllegalAccessException, WrongListReference {
        SQLiteDatabase database = OrmFactory.getDatabase();
        database.beginTransaction();
        boolean result = alter(entity, database);
        if (result)
            database.setTransactionSuccessful();
        database.endTransaction();
        return result;
    }

    private static boolean alter(OrmEntity entity, SQLiteDatabase database) throws WrongRightJoinReference, NotFindTableAnnotation, IllegalAccessException, WrongListReference {
        boolean result = false;
        Field primaryKeyField = getPrimaryKeyField(entity.getClass());
        primaryKeyField.setAccessible(true);
        if (primaryKeyField.get(entity) == null) {
            long id = database.insert(getTableName(entity.getClass()), "", getContentValues(entity));
            if (id > 0) {
                primaryKeyField.set(entity, id);
                result = true;
            }
        } else {
            int rowUpdated = database.update(getTableName(entity.getClass()), getContentValues(entity), String.format("%s = ?", getColumnName(primaryKeyField)), new String[]{primaryKeyField.get(entity).toString()});
            result = rowUpdated > 0;
        }
        if (result) {
            for (Field field : getForeignFields(entity.getClass())) {
                field.setAccessible(true);
                for (OrmEntity entityItem : (ArrayList<OrmEntity>) field.get(entity)) {
                    result = alter(entityItem, database);
                    if (!result)
                        return result;
                }
            }
        }
        return result;
    }

    private static ArrayList<Field> getForeignFields(Class<? extends OrmEntity> entityClass) throws WrongListReference {
        ArrayList<Field> result = new ArrayList<Field>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (checkForeign(entityClass, field))
                result.add(field);
        }
        return result;
    }

    private static ContentValues getContentValues(OrmEntity entity) throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference, IllegalAccessException {
        ContentValues values = new ContentValues();
        for (Field field : getAnnotationFieldsWithOutPrimaryKey(entity.getClass())) {
            field.setAccessible(true);
            String columnName = getColumnName(field);
            if (checkReference(field)) {
                OrmEntity referenceEntity = (OrmEntity) field.get(entity);
                Field primaryKeyField = getPrimaryKeyField(referenceEntity.getClass());
                primaryKeyField.setAccessible(true);
                values.put(columnName, (Long) primaryKeyField.get(referenceEntity));
            } else {
                String fieldType = field.getType().getSimpleName().toUpperCase();
                if (fieldType.equals("INT"))
                    values.put(columnName, field.getInt(entity));
                else if (fieldType.equals("LONG"))
                    values.put(columnName, (Long) field.get(entity));
                else if (fieldType.equals("STRING"))
                    values.put(columnName, (String) field.get(entity));
            }
        }
        return values;
    }

    public interface DefaultValues {
        void getDefaultValues(Class<? extends OrmEntity> entityClass, ArrayList<String> columns, ArrayList<ContentValues> valueList);
    }

    public static void CreateTable(Class<? extends OrmEntity> entityClass) throws NotFindPrimaryKeyField, WrongListReference, NotFindTableAnnotation, WrongRightJoinReference {
        String table;
        table = "CREATE TABLE " + getTableName(entityClass);
        table = table + " (" + getColumns(entityClass) + ");";
        OrmFactory.getDatabase().execSQL(table);
        InsertDefaultValues(entityClass);
    }

    static <T extends OrmEntity> ArrayList<T> getAllEntities(Class<T> entityClass) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongListReference, WrongRightJoinReference, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, DiscrepancyMappingColumns {
        Cursor cursor = OrmFactory.getDatabase().query(OrmUtils.getTableName(entityClass), null, "", null, "", "", "");
        if (cursor.moveToFirst()) {
            ArrayList<T> result = new ArrayList<T>();
            do {
                T entity = entityClass.getConstructor().newInstance();
                for (Field field : getAllAnnotationFields(entityClass)) {
                    int cursorIndex = cursor.getColumnIndex(getColumnName(field));
                    if (cursorIndex < 0)
                        throw new DiscrepancyMappingColumns(entityClass, field);
                    field.setAccessible(true);
                    field.set(entity, getValue(cursor, cursorIndex, field));
                }
                result.add(entity);
            } while (cursor.moveToNext());
            return result;
        }
        return null;
    }

    private static Object getValue(Cursor cursor, int cursorIndex, Field field) {
        String fieldType = field.getType().getSimpleName().toUpperCase();
        if (fieldType.equals("INT"))
            return cursor.getInt(cursorIndex);
        else if (fieldType.equals("LONG"))
            return cursor.getLong(cursorIndex);
        else if (fieldType.equals("STRING"))
            return cursor.getString(cursorIndex);
        return null;
    }

    private static void InsertDefaultValues(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongListReference, NotFindPrimaryKeyField, WrongRightJoinReference {
        ArrayList<DbColumn> columnsWithOutPrimaryKey = getColumnsWithOutPrimaryKey(entityClass);
        ArrayList<String> callBackColumns = new ArrayList<String>();
        for (DbColumn column : columnsWithOutPrimaryKey)
            callBackColumns.add(column.getName());
        ArrayList<ContentValues> valueList = new ArrayList<ContentValues>();
        OrmFactory.getHelper().getDefaultValues(entityClass, callBackColumns, valueList);
        if (valueList.size() > 0) {
            for (ContentValues values : valueList) {
                OrmFactory.getDatabase().insert(getTableName(entityClass), null, values);
            }
        }
    }

    private static ArrayList<DbColumn> getColumnsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongListReference, NotFindPrimaryKeyField, WrongRightJoinReference {
        ArrayList<DbColumn> columns = new ArrayList<DbColumn>();
        for (Field field : getAnnotationFieldsWithOutPrimaryKey(entityClass))
            columns.add(new DbColumn(getColumnName(field), getColumnType(field), getColumnAdditional(field)));
        return columns;
    }

    private static ArrayList<Field> getAnnotationFieldsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass) throws WrongListReference, WrongRightJoinReference, NotFindTableAnnotation {
        return getClassAnnotationFieldsWithOutPrimaryKey(entityClass, getTableJoinLeftClass(entityClass), new ArrayList<Field>(), 0);
    }

    private static ArrayList<Field> getClassAnnotationFieldsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass, Class<? extends OrmEntity> jointTo, ArrayList<Field> allFields, int level) throws WrongListReference {
        ArrayList<Field> classFields = new ArrayList<Field>();
        if (!entityClass.equals(jointTo)) {
            for (Field field : entityClass.getDeclaredFields()) {
                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null && (level == 0 || columnAnnotation.inherited()) && !columnAnnotation.primaryKey()) {
                    if (!checkForeign(entityClass, field))
                        classFields.add(field);
                }
            }
        }
        ArrayList<Field> fields = concatFields(allFields, classFields);
        Class superClass = entityClass.getSuperclass();
        if (OrmEntity.class.isAssignableFrom(superClass))
            return getClassAnnotationFieldsWithOutPrimaryKey(superClass, jointTo, fields, level + 1);
        return fields;
    }

    private static ArrayList<Field> concatFields(ArrayList<Field> allFields, ArrayList<Field> classFields) {
        if (classFields.size() == 0)
            return allFields;
        else {
            classFields.addAll(allFields);
            return classFields;
        }
    }

    private static String getTableName(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            String name = tableAnnotation.name();
            if (name.equals(""))
                name = entityClass.getSimpleName().toLowerCase();
            return name;
        } else
            throw new NotFindTableAnnotation(entityClass);
    }

    private static String getColumns(Class<? extends OrmEntity> entityClass) throws NotFindPrimaryKeyField, WrongListReference, NotFindTableAnnotation, WrongRightJoinReference {
        String columns = "";
        for (DbColumn column : getAllColumn(entityClass))
            columns += ", " + column.toString();
        if (columns.length() > 0)
            columns = columns.substring(2);
        return columns;
    }

    private static ArrayList<DbColumn> getAllColumn(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference, WrongListReference {
        ArrayList<DbColumn> result = new ArrayList<DbColumn>();
        for (Field field : getAllAnnotationFields(entityClass))
            result.add(new DbColumn(getColumnName(field), getColumnType(field), getColumnAdditional(field)));
        return result;
    }

    private static ArrayList<Field> getAllAnnotationFields(Class<? extends OrmEntity> entityClass) throws WrongRightJoinReference, NotFindTableAnnotation, WrongListReference {
        ArrayList<Field> result = new ArrayList<Field>();
        result.add(getPrimaryKeyField(entityClass));
        result.addAll(getAnnotationFieldsWithOutPrimaryKey(entityClass));
        return result;
    }

    private static Class<? extends OrmEntity> getTableJoinLeftClass(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongRightJoinReference {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null)
            throw new NotFindTableAnnotation(entityClass);
        Class<? extends OrmEntity> leftClass = tableAnnotation.leftJoinTo();
        if (!leftClass.equals(OrmEntity.class)) {
            tableAnnotation = leftClass.getAnnotation(Table.class);
            if (tableAnnotation == null)
                throw new NotFindTableAnnotation(leftClass);
            boolean findReference = false;
            for (Class rightClass : tableAnnotation.rightJoinTo()) {
                if (rightClass.equals(entityClass)) {
                    findReference = true;
                    break;
                }
            }
            if (!findReference)
                throw new WrongRightJoinReference(entityClass, leftClass);
            return leftClass;
        }
        return null;
    }

    private static String getColumnName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        String columnName = columnAnnotation.name();
        if (columnName.equals(""))
            columnName = field.getName().toLowerCase();
        return columnName;
    }

    private static String getColumnType(Field field) throws NotFindPrimaryKeyField, NotFindTableAnnotation {
        Class type = field.getType();
        String fieldType = type.getSimpleName().toUpperCase();
        if (fieldType.equals("INT") || fieldType.equals("LONG"))
            return "INTEGER";
        else if (fieldType.equals("STRING"))
            return "TEXT";
        return "";
    }

    private static boolean checkForeign(Class entityClass, Field field) throws WrongListReference {
        Class type = field.getType();
        if (type.getSimpleName().equals("ArrayList")) {
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            Class itemType = (Class) listType.getActualTypeArguments()[0];
            if (OrmEntity.class.isAssignableFrom(itemType)) {
                boolean okReference = false;
                for (Field classField : itemType.getDeclaredFields()) {
                    if (classField.getType().equals(entityClass)) {
                        okReference = true;
                        break;
                    }
                }
                if (!okReference)
                    throw new WrongListReference(itemType, entityClass);
            }
            return true;
        }
        return false;
    }

    private static DbColumn getPrimaryKeyColumn(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference {
        Field primaryField = getPrimaryKeyField(entityClass);
        return new DbColumn(getColumnName(primaryField), getColumnType(primaryField), getColumnAdditional(primaryField));
    }

    private static Field getPrimaryKeyField(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongRightJoinReference {
        return getPrimaryKeyField(entityClass, getTableJoinLeftClass(entityClass));
    }

    private static Field getPrimaryKeyField(Class entityClass, Class<? extends OrmEntity> joinTo) {
        if (!entityClass.equals(joinTo)) {
            for (Field field : entityClass.getDeclaredFields()) {
                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null && columnAnnotation.primaryKey())
                    return field;
            }
        }
        if (OrmEntity.class.isAssignableFrom(entityClass.getSuperclass()))
            return getPrimaryKeyField(entityClass.getSuperclass(), joinTo);
        return null;
    }

    private static boolean checkReference(Field field) {
        return OrmEntity.class.isAssignableFrom(field.getType());
    }

    private static String getColumnAdditional(Field field) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference {
        Class type = field.getType();
        if (checkReference(field)) {
            Field primaryKeyField = getPrimaryKeyField(type);
            if (primaryKeyField == null)
                throw new NotFindPrimaryKeyField(type);
            return "REFERENCES " + getTableName(type) + "(" + getColumnName(primaryKeyField) + ")";
        } else {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null && columnAnnotation.primaryKey())
                return "PRIMARY KEY ASC";
        }
        return "";
    }
}
