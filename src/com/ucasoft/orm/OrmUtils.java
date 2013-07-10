package com.ucasoft.orm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import com.ucasoft.orm.annotations.Column;
import com.ucasoft.orm.annotations.ReferenceAction;
import com.ucasoft.orm.annotations.Table;
import com.ucasoft.orm.exceptions.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 27.06.13
 * Time: 9:26
 */
public class OrmUtils {

    private static HashMap<Class<? extends OrmEntity>, List<OrmEntity>> cashedEntityLists = new HashMap<Class<? extends OrmEntity>, List<OrmEntity>>();

    private static List<OrmEntity> getCashedList(Class<? extends OrmEntity> entityClass){
        if (!cashedEntityLists.containsKey(entityClass))
            cashedEntityLists.put(entityClass, new ArrayList<OrmEntity>());
        return cashedEntityLists.get(entityClass);
    }

    static boolean delete(OrmEntity entity) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference, IllegalAccessException {
        SQLiteDatabase database = OrmFactory.getDatabase();
        Class<? extends OrmEntity> entityClass = entity.getClass();
        int rowDeleted = database.delete(getTableName(entityClass),String.format("%s = ?", getPrimaryKeyColumn(entityClass).getName()), new String[]{Long.toString(getEntityKey(entity))});
        return rowDeleted == 1;
    }

    static boolean alter(OrmEntity entity) throws WrongRightJoinReference, NotFindTableAnnotation, IllegalAccessException, WrongListReference {
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
        Class<? extends OrmEntity> entityClass = entity.getClass();
        Field primaryKeyField = getPrimaryKeyField(entityClass);
        primaryKeyField.setAccessible(true);
        if (primaryKeyField.get(entity) == null) {
            long id = database.insert(getTableName(entityClass), "", getContentValues(entity));
            if (id > 0) {
                primaryKeyField.set(entity, id);
                if (isCashed(entityClass))
                    getCashedList(entityClass).add(entity);
                result = true;
            }
        } else {
            int rowUpdated = database.update(getTableName(entityClass), getContentValues(entity), String.format("%s = ?", getColumnName(primaryKeyField)), new String[]{primaryKeyField.get(entity).toString()});
            result = rowUpdated > 0;
        }
        if (result) {
            for (Field field : getForeignFields(entityClass)){
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

    private static List<Field> getForeignFields(Class<? extends OrmEntity> entityClass) throws WrongListReference {
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
            }
            else{
                String fieldType = field.getType().getSimpleName().toUpperCase();
                if (fieldType.equals("INT"))
                    values.put(columnName, field.getInt(entity));
                else if (fieldType.equals("LONG"))
                    values.put(columnName, (Long) field.get(entity));
                else if (fieldType.equals("STRING"))
                    values.put(columnName, (String) field.get(entity));
                else if (fieldType.equals("DOUBLE"))
                    values.put(columnName, field.getDouble(entity));
                else if (fieldType.equals("DRAWABLE")){
                    Bitmap bitmap = ((BitmapDrawable)field.get(entity)).getBitmap();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    values.put(columnName, stream.toByteArray());
                }
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

    static <T extends OrmEntity> List<T> getAllEntities(Class<T> entityClass) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongListReference, WrongRightJoinReference, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, DiscrepancyMappingColumns {
        ArrayList<T> result;
        boolean cashed = isCashed(entityClass);
        if (!cashed || !cashedEntityLists.containsKey(entityClass)){
            result = new ArrayList<T>();
            Cursor cursor = OrmFactory.getDatabase().query(getTableName(entityClass), null, "", null, "", "", "");
        if (cursor.moveToFirst()) {
                do{
                    result.add(createEntity(entityClass, cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
            if (cashed)
                cashedEntityLists.put(entityClass, (List<OrmEntity>) result);
        }
        else {
            result = (ArrayList<T>) cashedEntityLists.get(entityClass);
            Field primaryField = getPrimaryKeyField(entityClass);
            primaryField.setAccessible(true);
            String inString = "";
            for (OrmEntity entity : cashedEntityLists.get(entityClass)){
                inString += Long.toString((Long) primaryField.get(entity)) + ", ";
            }
            inString = inString.substring(0, inString.length() - 2);
            Cursor cursor = OrmFactory.getDatabase().query(getTableName(entityClass), null, String.format("%s NOT IN (%s)", getColumnName(primaryField), inString), null, "", "", "");
            if (cursor.moveToFirst()){
                do{
                    result.add(createEntity(entityClass, cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return result;
    }

    static <T extends OrmEntity> List<T> getAllEntitiesForParent(Class<T> entityClass, T entity) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongListReference, WrongRightJoinReference, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, DiscrepancyMappingColumns {
            ArrayList<T> result = new ArrayList<T>();
        Long parentKey = getEntityKey(entity);
        Cursor cursor = OrmFactory.getDatabase().query(getTableName(entityClass), null, String.format("%s = ?", getParentColumn(entityClass, (Class<T>) entity.getClass()).getName()), new String[]{Long.toString(parentKey)}, "", "", "");
        if (cursor.moveToFirst()){
            do {
                result.add(createEntity(entityClass, entity, cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    private static <T extends OrmEntity> Long getEntityKey(T entity) throws NotFindTableAnnotation, WrongRightJoinReference, IllegalAccessException {
        Field parentKeyField = getPrimaryKeyField(entity.getClass());
        parentKeyField.setAccessible(true);
        return (Long)parentKeyField.get(entity);
    }

    private static <T extends OrmEntity> DbColumn getParentColumn(Class<T> entityClass, Class<T> parentEntityClass) throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference, NotFindPrimaryKeyField {
        for (Field field : getAnnotationFieldsWithOutPrimaryKey(entityClass)){
            if (field.getType().equals(parentEntityClass))
                return new DbColumn(getColumnName(field), getColumnType(field), getColumnAdditional(field));
        }
        return null;
    }

    private static <T extends OrmEntity> T createEntity(Class<T> entityClass, Cursor cursor) throws NotFindTableAnnotation, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, IllegalAccessException, WrongListReference, NotFindPrimaryKeyField, DiscrepancyMappingColumns {
        return createEntity(entityClass, null, cursor);
    }

    private static <T extends OrmEntity> T createEntity(Class<T> entityClass, T parentEntity, Cursor cursor) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, NotFindTableAnnotation, WrongListReference, DiscrepancyMappingColumns, NotFindPrimaryKeyField {
                T entity = entityClass.getConstructor().newInstance();
                for (Field field : getAllAnnotationFields(entityClass)) {
                    int cursorIndex = cursor.getColumnIndex(getColumnName(field));
                    if (cursorIndex < 0)
                        throw new DiscrepancyMappingColumns(entityClass, field);
                    field.setAccessible(true);
            if (checkReference(field))  {
                Class<? extends OrmEntity> fieldType = (Class<? extends OrmEntity>) field.getType();
                if (parentEntity != null && parentEntity.getClass().equals(fieldType))
                    field.set(entity, parentEntity);
                else {
                    long referenceEntityId = cursor.getLong(cursorIndex);
                    OrmEntity referenceEntity = null;
                    if (isCashed(fieldType)){
                        List<OrmEntity> cashedList = getCashedList(fieldType);
                        for(OrmEntity cashedEntity : cashedList){
                            if (getEntityKey(cashedEntity).equals(referenceEntityId)){
                                referenceEntity = cashedEntity;
                                break;
                            }
                        }
                        if (referenceEntity == null){
                            referenceEntity = getEntityByKey(fieldType, referenceEntityId);
                            cashedList.add(referenceEntity);
                        }
                    } else
                        referenceEntity = getEntityByKey(fieldType, referenceEntityId);
                    field.set(entity, referenceEntity);
                }
            }else
                    field.set(entity, getValue(cursor, cursorIndex, field));
                }
        for (Field field : getForeignFields(entityClass)) {
            field.setAccessible(true);
            field.set(entity, getAllEntitiesForParent((Class<OrmEntity>) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0], entity));
        }
        return entity;
    }

    private static <T extends OrmEntity> T getEntityByKey(Class<T> entityClass, Long entityId) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference, IllegalAccessException, DiscrepancyMappingColumns, InstantiationException, WrongListReference, NoSuchMethodException, InvocationTargetException {
        Cursor cursor = OrmFactory.getDatabase().query(getTableName(entityClass), null, String.format("%s = ?", getPrimaryKeyColumn(entityClass).getName()), new String[]{entityId.toString()}, "", "", "");
        if (cursor.moveToFirst()){
            return createEntity(entityClass, cursor);
        }
        cursor.close();
        return null;
    }

    private static boolean isCashed(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null)
            throw new NotFindTableAnnotation(entityClass);
        return tableAnnotation.cashedList();
    }

    private static Object getValue(Cursor cursor, int cursorIndex, Field field) {
        String fieldType = field.getType().getSimpleName().toUpperCase();
        if (fieldType.equals("INT"))
            return cursor.getInt(cursorIndex);
        else if (fieldType.equals("LONG"))
            return cursor.getLong(cursorIndex);
        else if (fieldType.equals("STRING"))
            return cursor.getString(cursorIndex);
        else if (fieldType.equals("DOUBLE"))
            return cursor.getDouble(cursorIndex);
        else if (fieldType.equals("DRAWABLE")){
            byte[] bytes = cursor.getBlob(cursorIndex);
            return new BitmapDrawable(null, BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
        }
        return null;
    }

    private static void InsertDefaultValues(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongListReference, NotFindPrimaryKeyField, WrongRightJoinReference {
        List<DbColumn> columnsWithOutPrimaryKey = getColumnsWithOutPrimaryKey(entityClass);
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

    private static List<DbColumn> getColumnsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongListReference, NotFindPrimaryKeyField, WrongRightJoinReference {
        ArrayList<DbColumn> columns = new ArrayList<DbColumn>();
        for (Field field : getAnnotationFieldsWithOutPrimaryKey(entityClass))
            columns.add(new DbColumn(getColumnName(field), getColumnType(field), getColumnAdditional(field)));
        return columns;
    }

    private static List<Field> getAnnotationFieldsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass) throws WrongListReference, WrongRightJoinReference, NotFindTableAnnotation {
        return getClassAnnotationFieldsWithOutPrimaryKey(entityClass, getTableJoinLeftClass(entityClass), new ArrayList<Field>(), 0);
    }

    private static List<Field> getClassAnnotationFieldsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass, Class<? extends OrmEntity> jointTo, List<Field> allFields, int level) throws WrongListReference {
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
        List<Field> fields = concatFields(allFields, classFields);
        Class superClass = entityClass.getSuperclass();
        if (OrmEntity.class.isAssignableFrom(superClass))
            return getClassAnnotationFieldsWithOutPrimaryKey(superClass, jointTo, fields, level + 1);
        return fields;
    }

    private static List<Field> concatFields(List<Field> allFields, List<Field> classFields) {
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

    private static List<DbColumn> getAllColumn(Class<? extends  OrmEntity> entityClass) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference, WrongListReference {
        ArrayList<DbColumn> result = new ArrayList<DbColumn>();
        for (Field field : getAllAnnotationFields(entityClass))
            result.add(new DbColumn(getColumnName(field), getColumnType(field), getColumnAdditional(field)));
        return result;
    }

    private static List<Field> getAllAnnotationFields(Class<? extends OrmEntity> entityClass) throws WrongRightJoinReference, NotFindTableAnnotation, WrongListReference {
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
        if (fieldType.equals("INT") || fieldType.equals("LONG") || checkReference(field))
            return "INTEGER";
        else if (fieldType.equals("STRING"))
            return "TEXT";
        else if (fieldType.equals("DOUBLE"))
            return "REAL";
        else if (fieldType.equals("DRAWABLE"))
            return "BLOB";
        return "";
    }

    private static boolean checkForeign(Class parentClass, Field field) throws WrongListReference {
        Class type = field.getType();
        if (type.getSimpleName().equals("List")) {
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            Class itemType = (Class) listType.getActualTypeArguments()[0];
            if (OrmEntity.class.isAssignableFrom(itemType)) {
                boolean okReference = false;
                for (Field classField : itemType.getDeclaredFields()) {
                    if (classField.getType().equals(parentClass)) {
                        okReference = true;
                        break;
                    }
                }
                if (!okReference)
                    throw new WrongListReference(itemType, parentClass);
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
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null){
            if (checkReference(field)) {
                Field primaryKeyField = getPrimaryKeyField(type);
                if (primaryKeyField == null)
                    throw new NotFindPrimaryKeyField(type);
                String reference = "REFERENCES " + getTableName(type) + "(" + getColumnName(primaryKeyField) + ")";
                if (columnAnnotation.onDelete() != ReferenceAction.NoAction)
                    reference += " ON DELETE " + columnAnnotation.onDelete().getAction();
                return reference;
            } else {
                if (columnAnnotation.primaryKey())
                    return "PRIMARY KEY ASC";
        }
        }
        return "";
    }
}
