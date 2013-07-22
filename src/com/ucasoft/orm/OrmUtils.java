package com.ucasoft.orm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import com.ucasoft.orm.annotations.Column;
import com.ucasoft.orm.annotations.ReferenceAction;
import com.ucasoft.orm.exceptions.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Date;
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
        if (OrmTableWorker.getTableJoinLeftClass(entityClass) != null)
            entityClass = OrmTableWorker.getTableJoinLeftClass(entityClass);
        int rowDeleted = database.delete(OrmTableWorker.getTableName(entityClass),String.format("%s = ?", getPrimaryKeyColumn(entityClass).getName()), new String[]{Long.toString(getEntityKey(entityClass, entity))});
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
        Class<? extends OrmEntity> joinLeftClass = OrmTableWorker.getTableJoinLeftClass(entity.getClass());
        OrmField primaryKeyField;
        if (joinLeftClass != null)
            primaryKeyField = OrmFieldWorker.getPrimaryKeyField(joinLeftClass);
        else
            primaryKeyField = OrmFieldWorker.getPrimaryKeyField(entityClass);
        primaryKeyField.setAccessible(true);
        if (primaryKeyField.get(entity) == null) {
            long id;
            if (joinLeftClass != null){
                ContentValues contentValues = getContentValues(entity, joinLeftClass);
                id = database.insert(OrmTableWorker.getTableName(joinLeftClass), "", contentValues);
                if (id > 0){
                    contentValues = getContentValues(entity, null);
                    contentValues.put(OrmFieldWorker.getPrimaryKeyField(entityClass).getName(), id);
                    long leftJoinResult = database.insert(OrmTableWorker.getTableName(entityClass), "", contentValues);
                    if (leftJoinResult < 0)
                        return false;
                }
            }
            else
                id = database.insert(OrmTableWorker.getTableName(entityClass), "", getContentValues(entity));
            if (id > 0) {
                primaryKeyField.set(entity, id);
                if (OrmTableWorker.isCashed(entityClass))
                    getCashedList(entityClass).add(entity);
                result = true;
            }
        } else {
            int rowUpdated = 0;
            if (joinLeftClass != null) {
                rowUpdated= database.update(OrmTableWorker.getTableName(joinLeftClass), getContentValues(entity, joinLeftClass), String.format("%s = ?", getColumnName(primaryKeyField)), new String[]{primaryKeyField.get(entity).toString()});
                primaryKeyField = OrmFieldWorker.getPrimaryKeyField(entityClass);
            }
            rowUpdated += database.update(OrmTableWorker.getTableName(entityClass), getContentValues(entity, null), String.format("%s = ?", getColumnName(primaryKeyField)), new String[]{primaryKeyField.get(entity).toString()});
            result = rowUpdated > 0;
        }
        if (result) {
            List<OrmField> foreignFields = new ArrayList<OrmField>(OrmFieldWorker.getForeignFields(entityClass));
            if (joinLeftClass != null)
                foreignFields.addAll(0, OrmFieldWorker.getForeignFields(joinLeftClass));
            for (OrmField field : foreignFields){
                field.setAccessible(true);
                for(OrmEntity entityItem : (List<OrmEntity>)field.get(entity)){
                    result = alter(entityItem, database);
                    if (!result)
                        return false;
                }
            }
        }
        return result;
    }

    private static ContentValues getContentValues(OrmEntity entity) throws IllegalAccessException, WrongRightJoinReference, WrongListReference, NotFindTableAnnotation {
        return getContentValues(entity, null);
    }

    private static ContentValues getContentValues(OrmEntity entity, Class<? extends OrmEntity> joinLeftClass) throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference, IllegalAccessException {
        ContentValues values = new ContentValues();
        Class<? extends OrmEntity> findToClass;
        if (joinLeftClass != null)
            findToClass = joinLeftClass;
        else
            findToClass = entity.getClass();
        for (OrmField field : OrmFieldWorker.getAnnotationFieldsWithOutPrimaryKey(findToClass)){
            field.setAccessible(true);
            String columnName = getColumnName(field);
            if (checkReference(field)) {
                OrmEntity referenceEntity = (OrmEntity) field.get(entity);
                if (referenceEntity != null){
                OrmField primaryKeyField = OrmFieldWorker.getPrimaryKeyField(referenceEntity.getClass());
                primaryKeyField.setAccessible(true);
                values.put(columnName, (Long) primaryKeyField.get(referenceEntity));
            }
            }
            else{
                String fieldType = field.getType().getSimpleName().toUpperCase();
                if (fieldType.equals("INT"))
                    values.put(columnName, field.getInt(entity));
                else if (fieldType.equals("LONG"))
                    values.put(columnName, (Long) field.get(entity));
                else if (fieldType.equals("DATE"))
                    values.put(columnName, ((Date)field.get(entity)).getTime());
                else if (fieldType.equals("STRING"))
                    values.put(columnName, (String) field.get(entity));
                else if (fieldType.equals("DOUBLE"))
                    values.put(columnName, field.getDouble(entity));
                else if (fieldType.equals("DRAWABLE")){
                    Bitmap bitmap = ((BitmapDrawable)field.get(entity)).getBitmap();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    values.put(columnName, stream.toByteArray());
                } else if (fieldType.equals("DOCUMENT"))
                    values.put(columnName, getStringFromDocument((Document)field.get(entity)));
            }
        }
        return values;
    }

    private static String getStringFromDocument(Document document) {
        try{
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(new DOMSource(document), result);
            return writer.toString();
        } catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    static <T extends OrmEntity> List<T> getEntitiesWhereEx(Class<T> entityClass, String where, String[] params) throws IllegalAccessException, WrongJoinLeftReference, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, NotFindTableAnnotation, DiscrepancyMappingColumns, NotFindPrimaryKeyField, WrongListReference {
        return getEntitiesEx(entityClass, where, params, false);
    }

    public interface DefaultValues {
        void getDefaultValues(Class<? extends OrmEntity> entityClass, ArrayList<String> columns, ArrayList<ContentValues> valueList);
    }

    public static void CreateTable(Class<? extends OrmEntity> entityClass) throws NotFindPrimaryKeyField, WrongListReference, NotFindTableAnnotation, WrongRightJoinReference {
        String table;
        table = "CREATE TABLE " + OrmTableWorker.getTableName(entityClass);
        table = table + " (" + getColumns(entityClass);
        Class<? extends OrmEntity> leftJoinTo = OrmTableWorker.getTableJoinLeftClass(entityClass);
        if (leftJoinTo != null)
            table += ", PRIMARY KEY (" + OrmFieldWorker.getPrimaryKeyField(entityClass).getName() + "));";
        else
            table += ");";
        OrmFactory.getDatabase().execSQL(table);
        InsertDefaultValues(entityClass);
    }

    @Deprecated
    static <T extends OrmEntity> List<T> getEntitiesWhere(Class<T> entityClass, String where, String[] args) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, NotFindTableAnnotation, WrongListReference, NotFindPrimaryKeyField, DiscrepancyMappingColumns {
        return getEntities(entityClass, where, args);
    }

    @Deprecated
    static <T extends OrmEntity> List<T> getAllEntities(Class<T> entityClass) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongListReference, WrongRightJoinReference, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, DiscrepancyMappingColumns {
        return getEntities(entityClass, "", null);
    }

    public static <T extends OrmEntity> List<T> getAllEntitiesEx(Class<T> entityClass, boolean includeLeftChild) throws NotFindTableAnnotation, WrongRightJoinReference, NoSuchMethodException, NotFindPrimaryKeyField, DiscrepancyMappingColumns, InstantiationException, WrongListReference, IllegalAccessException, InvocationTargetException, WrongJoinLeftReference {
        return getEntitiesEx(entityClass, null, null, includeLeftChild);
    }

    //TODO Необходимо добавить поддержку Cashed List'ов!
    public static <T extends OrmEntity> List<T> getEntitiesEx(Class<T> entityClass, String where, String[] params, boolean includeLeftChild) throws NotFindTableAnnotation, WrongRightJoinReference, NoSuchMethodException, NotFindPrimaryKeyField, DiscrepancyMappingColumns, InstantiationException, WrongListReference, IllegalAccessException, InvocationTargetException, WrongJoinLeftReference {
        ArrayList<T> result = new ArrayList<T>();
        List<Class<? extends OrmEntity>> rightToClasses = OrmTableWorker.getTableRightJoinClasses(entityClass);
        Class<? extends OrmEntity> joinLeftClass = OrmTableWorker.getTableJoinLeftClass(entityClass);
        String sql = "SELECT * FROM " + OrmTableWorker.getTableName(entityClass);
        if (joinLeftClass != null) {
            sql += " LEFT JOIN " + OrmTableWorker.getTableName(joinLeftClass) + " ON " + OrmFieldWorker.getPrimaryKeyField(entityClass).getName() + " = " + OrmFieldWorker.getPrimaryKeyField(joinLeftClass).getName();
        }
        if (where != null){
            sql += String.format(" WHERE " + where.replace("?", "%s"), params).replace("[", "").replace("]", "");
        }
        if (rightToClasses.size() > 0){
            String notExists = null;
            if (where == null)
                notExists = " WHERE ";
            for (Class<? extends OrmEntity> rightEntityClass : rightToClasses){
                if (notExists != null && !notExists.equals(" WHERE "))
                    notExists += "AND ";
                notExists += "NOT EXISTS(SELECT 1 FROM " + OrmTableWorker.getTableName(rightEntityClass) + " WHERE " + OrmFieldWorker.getPrimaryKeyField(rightEntityClass).getName() + " = " + OrmFieldWorker.getPrimaryKeyField(entityClass).getName() + ") ";
            }
            sql += notExists;
        }
        Cursor cursor = OrmFactory.getDatabase().rawQuery(sql, null);
        if (cursor.moveToFirst()){
            do{
                result.add(createEntity(entityClass, cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        if (includeLeftChild){
            for(Class<? extends OrmEntity> rightEntityClass : rightToClasses){
                sql = "SELECT * FROM " + OrmTableWorker.getTableName(rightEntityClass) + " LEFT JOIN " + OrmTableWorker.getTableName(entityClass) + " ON " + OrmFieldWorker.getPrimaryKeyField(rightEntityClass).getName() + " = " + OrmFieldWorker.getPrimaryKeyField(entityClass).getName();
                cursor = OrmFactory.getDatabase().rawQuery(sql, null);
                if (cursor.moveToFirst()){
                    do{
                        result.add((T) createEntity(rightEntityClass, cursor));
                    } while (cursor.moveToNext());
                }
            }
        }
        return result;
    }

    /**
     * @deprecated Will be delete when {@link #getEntitiesEx(Class, String, String[], boolean)} is create completed
     */
    @Deprecated
    private static <T extends OrmEntity> List<T> getEntities(Class<T> entityClass, String where, String[] args) throws NotFindTableAnnotation, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, IllegalAccessException, WrongListReference, NotFindPrimaryKeyField, DiscrepancyMappingColumns {
        ArrayList<T> result;
        boolean cashed = OrmTableWorker.isCashed(entityClass);
        if (!cashed || !cashedEntityLists.containsKey(entityClass)){
            result = new ArrayList<T>();
            Cursor cursor = OrmFactory.getDatabase().query(OrmTableWorker.getTableName(entityClass), null, where, args, "", "", "");
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
            ArrayList<T> tempList = (ArrayList<T>) cashedEntityLists.get(entityClass);
            OrmField primaryField = OrmFieldWorker.getPrimaryKeyField(entityClass);
            primaryField.setAccessible(true);
            if (where.equals("")){
            String inString = "";
            for (OrmEntity entity : cashedEntityLists.get(entityClass)){
                inString += Long.toString((Long) primaryField.get(entity)) + ", ";
            }
            inString = inString.substring(0, inString.length() - 2);
                String localWhere = "%s NOT IN (%s)";
                Cursor cursor = OrmFactory.getDatabase().query(OrmTableWorker.getTableName(entityClass), null, String.format(localWhere, getColumnName(primaryField), inString), args, "", "", "");
            if (cursor.moveToFirst()){
                do{
                        tempList.add(createEntity(entityClass, cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
                result = tempList;
            } else {
                result = new ArrayList<T>();
                String primaryKey = getColumnName(primaryField);
                Cursor cursor = OrmFactory.getDatabase().query(OrmTableWorker.getTableName(entityClass), null, where, args, "", "", "");
                if (cursor.moveToFirst()){
                    do{
                        Long id = cursor.getLong(cursor.getColumnIndex(primaryKey));
                        boolean findCash = false;
                        for (T entity : tempList){
                            if (primaryField.get(entity) == id){
                                result.add(entity);
                                findCash = true;
                                break;
                            }
                        }
                        if (!findCash){
                            T entity = createEntity(entityClass, cursor);
                            tempList.add(entity);
                            result.add(entity);
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        }
        return result;
    }

    static <T extends OrmEntity> List<T> getAllEntitiesForParent(Class<OrmEntity> entityClass, Class<T> parentEntityClass, OrmEntity entity) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongListReference, WrongRightJoinReference, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, DiscrepancyMappingColumns {
            ArrayList<T> result = new ArrayList<T>();
        Long parentKey = getEntityKey(parentEntityClass, entity);
        Cursor cursor = OrmFactory.getDatabase().query(OrmTableWorker.getTableName(entityClass), null, String.format("%s = ?", getParentColumn(entityClass, parentEntityClass).getName()), new String[]{Long.toString(parentKey)}, "", "", "");
        if (cursor.moveToFirst()){
            do {
                result.add((T) createEntity(entityClass, entity, cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    private static <T extends OrmEntity> Long getEntityKey(T entity) throws NotFindTableAnnotation, IllegalAccessException, WrongRightJoinReference {
        return getEntityKey(entity.getClass(), entity);
    }

    private static <T extends OrmEntity> Long getEntityKey(Class<? extends OrmEntity> entityClass, T entity) throws NotFindTableAnnotation, WrongRightJoinReference, IllegalAccessException {
        OrmField parentKeyField = OrmFieldWorker.getPrimaryKeyField(entityClass);
        parentKeyField.setAccessible(true);
        return (Long)parentKeyField.get(entity);
    }

    private static <T extends OrmEntity> DbColumn getParentColumn(Class<OrmEntity> entityClass, Class<T> parentEntityClass) throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference, NotFindPrimaryKeyField {
        for (OrmField field : OrmFieldWorker.getAnnotationFieldsWithOutPrimaryKey(entityClass)){
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
        if (OrmTableWorker.getTableJoinLeftClass(entityClass) != null)
            buildEntity((Class<OrmEntity>) OrmTableWorker.getTableJoinLeftClass(entityClass), parentEntity, cursor, entity);
        buildEntity(entityClass, parentEntity, cursor, entity);
        return entity;
    }

    private static <T extends OrmEntity> void buildEntity(Class<T> entityClass, T parentEntity, Cursor cursor, T entity) throws WrongRightJoinReference, NotFindTableAnnotation, WrongListReference, DiscrepancyMappingColumns, IllegalAccessException, NotFindPrimaryKeyField, InstantiationException, NoSuchMethodException, InvocationTargetException {
        for(OrmField field : OrmFieldWorker.getAllAnnotationFields(entityClass)){
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
                    if (OrmTableWorker.isCashed(fieldType)){
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
        for (OrmField field : OrmFieldWorker.getForeignFields(entityClass)) {
            field.setAccessible(true);
            field.set(entity, getAllEntitiesForParent((Class<OrmEntity>) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0], entityClass, entity));
        }
    }

    private static <T extends OrmEntity> T getEntityByKey(Class<T> entityClass, Long entityId) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference, IllegalAccessException, DiscrepancyMappingColumns, InstantiationException, WrongListReference, NoSuchMethodException, InvocationTargetException {
        Cursor cursor = OrmFactory.getDatabase().query(OrmTableWorker.getTableName(entityClass), null, String.format("%s = ?", getPrimaryKeyColumn(entityClass).getName()), new String[]{entityId.toString()}, "", "", "");
        if (cursor.moveToFirst()){
            return createEntity(entityClass, cursor);
        }
        cursor.close();
        return null;
    }

    private static Object getValue(Cursor cursor, int cursorIndex, OrmField field) {
        String fieldType = field.getType().getSimpleName().toUpperCase();
        if (fieldType.equals("INT"))
            return cursor.getInt(cursorIndex);
        else if (fieldType.equals("LONG"))
            return cursor.getLong(cursorIndex);
        else if (fieldType.equals("DATE"))
            return new Date(cursor.getLong(cursorIndex));
        else if (fieldType.equals("STRING"))
            return cursor.getString(cursorIndex);
        else if (fieldType.equals("DOUBLE"))
            return cursor.getDouble(cursorIndex);
        else if (fieldType.equals("DRAWABLE")){
            byte[] bytes = cursor.getBlob(cursorIndex);
            return new BitmapDrawable(null, BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
        } else if(fieldType.equals("DOCUMENT"))
            return getDocumentFromString(cursor.getString(cursorIndex));
        return null;
        }

    private static Document getDocumentFromString(String string) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(string)));
        } catch (Exception e) {
            e.printStackTrace();
        return null;
    }
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
                OrmFactory.getDatabase().insert(OrmTableWorker.getTableName(entityClass), null, values);
            }
        }
    }

    private static List<DbColumn> getColumnsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongListReference, NotFindPrimaryKeyField, WrongRightJoinReference {
        ArrayList<DbColumn> columns = new ArrayList<DbColumn>();
        for(OrmField field : OrmFieldWorker.getAnnotationFieldsWithOutPrimaryKey(entityClass))
            columns.add(new DbColumn(getColumnName(field), getColumnType(field), getColumnAdditional(field)));
        return columns;
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
        for(OrmField field : OrmFieldWorker.getAllAnnotationFields(entityClass))
            result.add(new DbColumn(getColumnName(field), getColumnType(field), getColumnAdditional(field)));
        return result;
    }

    private static String getColumnName(OrmField field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        String columnName = columnAnnotation.name();
        if (columnName.equals(""))
            columnName = field.getName().toLowerCase();
        return columnName;
    }

    private static String getColumnType(OrmField field) throws NotFindPrimaryKeyField, NotFindTableAnnotation {
        Class type = field.getType();
        String fieldType = type.getSimpleName().toUpperCase();
        if (fieldType.equals("INT") || fieldType.equals("LONG") || fieldType.equals("DATE") || checkReference(field))
            return "INTEGER";
        else if (fieldType.equals("STRING") || fieldType.equals("DOCUMENT"))
            return "TEXT";
        else if (fieldType.equals("DOUBLE"))
            return "REAL";
        else if (fieldType.equals("DRAWABLE"))
            return "BLOB";
        return "";
    }

    private static DbColumn getPrimaryKeyColumn(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference {
        OrmField primaryField = OrmFieldWorker.getPrimaryKeyField(entityClass);
        return new DbColumn(getColumnName(primaryField), getColumnType(primaryField), getColumnAdditional(primaryField));
    }

    private static boolean checkReference(OrmField field){
        return OrmEntity.class.isAssignableFrom(field.getType());
    }

    private static String getColumnAdditional(OrmField field) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference {
        Class type = field.getType();
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null){
            if (checkReference(field)) {
                OrmField primaryKeyField = OrmFieldWorker.getPrimaryKeyField(type);
                if (primaryKeyField == null)
                    throw new NotFindPrimaryKeyField(type);
                String reference = "REFERENCES " + OrmTableWorker.getTableName(type) + "(" + getColumnName(primaryKeyField) + ")";
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
