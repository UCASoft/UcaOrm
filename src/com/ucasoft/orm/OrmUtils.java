package com.ucasoft.orm;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
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
import java.lang.reflect.*;
import java.util.*;

/**
 * Created by UCASoft.
 * User: Antonov Sergey
 * Date: 27.06.13
 * Time: 9:26
 */
@TargetApi(value = Build.VERSION_CODES.GINGERBREAD)
public class OrmUtils {

    private final static HashMap<Class<? extends OrmEntity>, List<OrmEntity>> cashedEntityLists = new HashMap<Class<? extends OrmEntity>, List<OrmEntity>>();

    private static List<OrmEntity> getCashedList(Class<? extends OrmEntity> entityClass){
        if (!cashedEntityLists.containsKey(entityClass))
            cashedEntityLists.put(entityClass, new ArrayList<OrmEntity>());
        return cashedEntityLists.get(entityClass);
    }

    private static <T extends OrmEntity> T findCashedEntity(Class<T> entityClass, long id) throws WrongRightJoinReference, NotFindTableAnnotation, IllegalAccessException {
        List<OrmEntity> cashedList = getCashedList(entityClass);
        OrmField field = OrmFieldWorker.getPrimaryKeyField(entityClass, true);
        for (OrmEntity entity : cashedList) {
            if (field.get(entity).equals(id))
                return (T) entity;
        }
        return null;
    }

    private static <T extends OrmEntity> T buildCashedEntity(Class<T> entityClass, Cursor cursor) throws WrongRightJoinReference, NotFindTableAnnotation, IllegalAccessException, NoSuchMethodException, NotFindPrimaryKeyField, DiscrepancyMappingColumns, InstantiationException, WrongListReference, InvocationTargetException, WrongJoinLeftReference {
        OrmField field = OrmFieldWorker.getPrimaryKeyField(entityClass, true);
        Long id = cursor.getLong(cursor.getColumnIndex(field.getName()));
        T result = findCashedEntity(entityClass, id);
        if (result == null){
            result = createEntity(entityClass, cursor);
            getCashedList(entityClass).add(result);
        }
        return result;
    }

    static boolean delete(OrmEntity entity) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference, IllegalAccessException {
        SQLiteDatabase database = OrmFactory.getDatabase();
        Class<? extends OrmEntity> entityClass = entity.getClass();
        if (OrmTableWorker.getTableJoinLeftClass(entityClass) != null)
            entityClass = OrmTableWorker.getTableJoinLeftClass(entityClass);
        int rowDeleted = database.delete(OrmTableWorker.getTableName(entityClass),String.format("%s = ?", getPrimaryKeyColumn(entityClass).getName()), new String[]{Long.toString(getEntityKey(entityClass, entity))});
        return rowDeleted == 1;
    }

    static boolean alterInTransaction(OrmEntity entity) throws WrongRightJoinReference, NotFindTableAnnotation, IllegalAccessException, WrongListReference {
        SQLiteDatabase database = OrmFactory.getDatabase();
        return alter(entity, database);
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
                rowUpdated = database.update(OrmTableWorker.getTableName(joinLeftClass), getContentValues(entity, joinLeftClass), String.format("%s = ?", DbColumn.getColumnName(primaryKeyField)), new String[]{primaryKeyField.get(entity).toString()});
                primaryKeyField = OrmFieldWorker.getPrimaryKeyField(entityClass);
            }
            rowUpdated += database.update(OrmTableWorker.getTableName(entityClass), getContentValues(entity, null), String.format("%s = ?", DbColumn.getColumnName(primaryKeyField)), new String[]{primaryKeyField.get(entity).toString()});
            result = rowUpdated > 0;
        }
        if (result) {
            List<OrmField> foreignFields = new ArrayList<OrmField>(OrmFieldWorker.getForeignFields(entityClass));
            if (joinLeftClass != null)
                foreignFields.addAll(0, OrmFieldWorker.getForeignFields(joinLeftClass));
            for (OrmField field : foreignFields){
                field.setAccessible(true);
                for(OrmEntity entityItem : (List<OrmEntity>)field.get(entity)) {
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
        for (OrmField field : OrmFieldWorker.getAnnotationFieldsWithOutPrimaryKey(findToClass)) {
            field.setAccessible(true);
            String columnName = DbColumn.getColumnName(field);
            if (DbColumn.isReferenceField(field)) {
                OrmEntity referenceEntity = (OrmEntity) field.get(entity);
                if (referenceEntity != null){
                    OrmField primaryKeyField = OrmFieldWorker.getPrimaryKeyField(referenceEntity.getClass());
                    primaryKeyField.setAccessible(true);
                    values.put(columnName, (Long) primaryKeyField.get(referenceEntity));
                }
            }
            else {
                Class type = field.getType();
                if (type.isArray()) {
                    if (type.getComponentType().isPrimitive()) {
                        String componentType = type.getComponentType().getSimpleName().toUpperCase();
                        String data = "";
                        if (componentType.equals("INT"))
                            data = Arrays.toString((int[]) field.get(entity));
                        values.put(columnName, data.substring(1, data.length() - 1));
                    }
                } else {
                    String fieldType = type.getSimpleName().toUpperCase();
                    if (fieldType.equals("INT"))
                        values.put(columnName, field.getInt(entity));
                    else if (fieldType.equals("LONG"))
                        values.put(columnName, (Long) field.get(entity));
                    else if (fieldType.equals("DATE"))
                        values.put(columnName, ((Date) field.get(entity)).getTime());
                    else if (fieldType.equals("BOOLEAN"))
                        values.put(columnName, ((Boolean) field.get(entity)) ? 1 : 0);
                    else if (fieldType.equals("STRING"))
                        values.put(columnName, (String) field.get(entity));
                    else if (fieldType.equals("DOUBLE"))
                        values.put(columnName, field.getDouble(entity));
                    else if (fieldType.equals("DRAWABLE")) {
                        Bitmap bitmap = ((BitmapDrawable) field.get(entity)).getBitmap();
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        values.put(columnName, stream.toByteArray());
                    } else if (fieldType.equals("DOCUMENT"))
                        values.put(columnName, getStringFromDocument((Document) field.get(entity)));
                }
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
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    static <T extends OrmEntity> List<T> getEntitiesWhere(Class<T> entityClass, String where, String[] params, String order) throws IllegalAccessException, WrongJoinLeftReference, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, NotFindTableAnnotation, DiscrepancyMappingColumns, NotFindPrimaryKeyField, WrongListReference {
        return getEntities(entityClass, where, params, order, false);
    }

    public static OrmUpdater UpdateTable(Class<? extends OrmEntity> entityClass) {
        return new OrmUpdater(entityClass);
    }

    static List<DbColumn> getBaseColumns(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation {
        List<DbColumn> result = new ArrayList<DbColumn>();
        String sql = String.format("PRAGMA table_info(%s);", OrmTableWorker.getTableName(entityClass));
        Cursor cursor = OrmFactory.getDatabase().rawQuery(sql, null);
        try {
            if (cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndexOrThrow("name");
                int typeIndex = cursor.getColumnIndexOrThrow("type");
                do {
                    result.add(new DbColumn(cursor.getString(nameIndex), cursor.getString(typeIndex), ""));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    interface DefaultValues {
        void getDefaultValues(Class<? extends OrmEntity> entityClass, List<OrmEntity> valueList);
    }

    public static void CreateTable(Class<? extends OrmEntity> entityClass) throws NotFindPrimaryKeyField, WrongListReference, NotFindTableAnnotation, WrongRightJoinReference, IllegalAccessException {
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

    static <T extends OrmEntity> List<T> getAllEntities(Class<T> entityClass) throws NotFindTableAnnotation, WrongRightJoinReference, NoSuchMethodException, NotFindPrimaryKeyField, DiscrepancyMappingColumns, InstantiationException, WrongListReference, IllegalAccessException, InvocationTargetException, WrongJoinLeftReference {
        return getEntities(entityClass, null, null, null, false);
    }

    static <T extends OrmEntity> List<T> getAllEntities(Class<T> entityClass, boolean includeLeftChild) throws NotFindTableAnnotation, WrongRightJoinReference, NoSuchMethodException, NotFindPrimaryKeyField, DiscrepancyMappingColumns, InstantiationException, WrongListReference, IllegalAccessException, InvocationTargetException, WrongJoinLeftReference {
        return getEntities(entityClass, null, null, null, includeLeftChild);
    }

    static <T extends OrmEntity> List<T> getEntities(Class<T> entityClass, String where, String[] params, String order, boolean includeLeftChild) throws NotFindTableAnnotation, WrongRightJoinReference, NoSuchMethodException, NotFindPrimaryKeyField, DiscrepancyMappingColumns, InstantiationException, WrongListReference, IllegalAccessException, InvocationTargetException, WrongJoinLeftReference {
        ArrayList<T> result = new ArrayList<T>();
        boolean cashed = OrmTableWorker.isCashed(entityClass);
        List<Class<? extends OrmEntity>> rightToClasses = OrmTableWorker.getTableRightJoinClasses(entityClass);
        Class<? extends OrmEntity> joinLeftClass = OrmTableWorker.getTableJoinLeftClass(entityClass);
        String sql = "SELECT * FROM " + OrmTableWorker.getTableName(entityClass);
        if (joinLeftClass != null) {
            sql += " LEFT JOIN " + OrmTableWorker.getTableName(joinLeftClass) + " ON " + OrmFieldWorker.getPrimaryKeyField(entityClass).getName() + " = " + OrmFieldWorker.getPrimaryKeyField(joinLeftClass).getName();
        }
        if (where != null)
            sql += String.format(" WHERE " + where.replace("?", "%s"), params).replace("[", "").replace("]", "");
        if (rightToClasses.size() > 0) {
            String notExists;
            if (where == null)
                notExists = " WHERE ";
            else
                notExists = " AND ";
            for (Class<? extends OrmEntity> rightEntityClass : rightToClasses){
                if (notExists.length() > 7)
                    notExists += "AND ";
                notExists += "NOT EXISTS(SELECT 1 FROM " + OrmTableWorker.getTableName(rightEntityClass) + " WHERE " + OrmFieldWorker.getPrimaryKeyField(rightEntityClass).getName() + " = " + OrmFieldWorker.getPrimaryKeyField(entityClass).getName() + ") ";
            }
            sql += notExists;
        }
        if (order != null)
            sql += " ORDER BY " + order;
        Cursor cursor = OrmFactory.getDatabase().rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            if (cashed){
                do {
                    result.add(buildCashedEntity(entityClass, cursor));
                } while (cursor.moveToNext());
            } else {
                do{
                    result.add(createEntity(entityClass, cursor));
                } while (cursor.moveToNext());
            }
        }
        cursor.close();
        if (includeLeftChild){
            for(Class<? extends OrmEntity> rightEntityClass : rightToClasses) {
                sql = "SELECT * FROM " + OrmTableWorker.getTableName(rightEntityClass) + " LEFT JOIN " + OrmTableWorker.getTableName(entityClass) + " ON " + OrmFieldWorker.getPrimaryKeyField(rightEntityClass).getName() + " = " + OrmFieldWorker.getPrimaryKeyField(entityClass).getName();
                if (where != null)
                    sql += String.format(" WHERE " + where.replace("?", "%s"), params).replace("[", "").replace("]", "");
                if (order != null)
                    sql += " ORDER BY " + order;
                cursor = OrmFactory.getDatabase().rawQuery(sql, null);
                if (cursor.moveToFirst()){
                    if (cashed) {
                        do{
                            result.add((T) buildCashedEntity(rightEntityClass, cursor));
                        } while (cursor.moveToNext());
                    } else {
                        do{
                            result.add((T) createEntity(rightEntityClass, cursor));
                        } while (cursor.moveToNext());
                    }
                }
                cursor.close();
            }
        }
        return result;
    }

    static <T extends OrmEntity> List<T> getAllEntitiesForParent(Class<OrmEntity> entityClass, Class<T> parentEntityClass, OrmEntity entity) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongListReference, WrongRightJoinReference, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, DiscrepancyMappingColumns, WrongJoinLeftReference {
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

    private static <T extends OrmEntity> Long getEntityKey(Class<? extends OrmEntity> entityClass, T entity) throws NotFindTableAnnotation, WrongRightJoinReference, IllegalAccessException {
        OrmField parentKeyField = OrmFieldWorker.getPrimaryKeyField(entityClass);
        parentKeyField.setAccessible(true);
        return (Long)parentKeyField.get(entity);
    }

    private static <T extends OrmEntity> DbColumn getParentColumn(Class<OrmEntity> entityClass, Class<T> parentEntityClass) throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference, NotFindPrimaryKeyField {
        for (OrmField field : OrmFieldWorker.getAnnotationFieldsWithOutPrimaryKey(entityClass)){
            if (field.getType().equals(parentEntityClass))
                return new DbColumn(field);
        }
        return null;
    }

    private static <T extends OrmEntity> T createEntity(Class<T> entityClass, Cursor cursor) throws NotFindTableAnnotation, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, IllegalAccessException, WrongListReference, NotFindPrimaryKeyField, DiscrepancyMappingColumns, WrongJoinLeftReference {
        return createEntity(entityClass, null, cursor);
    }

    private static <T extends OrmEntity> T createEntity(Class<T> entityClass, T parentEntity, Cursor cursor) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, NotFindTableAnnotation, WrongListReference, DiscrepancyMappingColumns, NotFindPrimaryKeyField, WrongJoinLeftReference {
        T entity = entityClass.getConstructor().newInstance();
        if (OrmTableWorker.getTableJoinLeftClass(entityClass) != null)
            buildEntity((Class<OrmEntity>) OrmTableWorker.getTableJoinLeftClass(entityClass), parentEntity, cursor, entity);
        buildEntity(entityClass, parentEntity, cursor, entity);
        return entity;
    }

    private static <T extends OrmEntity> void buildEntity(Class<T> entityClass, T parentEntity, Cursor cursor, T entity) throws WrongRightJoinReference, NotFindTableAnnotation, WrongListReference, DiscrepancyMappingColumns, IllegalAccessException, NotFindPrimaryKeyField, InstantiationException, NoSuchMethodException, InvocationTargetException, WrongJoinLeftReference {
        Class<? extends OrmEntity> tableJoinLeftClass = OrmTableWorker.getTableJoinLeftClass(entityClass);
        for(OrmField field : OrmFieldWorker.getAllAnnotationFields(entityClass)){
            int cursorIndex = cursor.getColumnIndex(DbColumn.getColumnName(field));
            if (cursorIndex < 0)
                throw new DiscrepancyMappingColumns(entityClass, field);
            field.setAccessible(true);
            if (DbColumn.isReferenceField(field) && !field.getType().equals(tableJoinLeftClass)) {
                Class<? extends OrmEntity> fieldType = (Class<? extends OrmEntity>) field.getType();
                if (parentEntity != null && fieldType.isAssignableFrom(parentEntity.getClass()))
                    field.set(entity, parentEntity);
                else {
                    long referenceEntityId = cursor.getLong(cursorIndex);
                    if (referenceEntityId > 0)
                        field.set(entity, getEntityByKey(fieldType, referenceEntityId));
                }
            } else
                field.set(entity, getValue(cursor, cursorIndex, field));
        }
        for (OrmField field : OrmFieldWorker.getForeignFields(entityClass)) {
            field.setAccessible(true);
            field.set(entity, getAllEntitiesForParent((Class<OrmEntity>) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0], entityClass, entity));
        }
    }

    private static <T extends OrmEntity> T getEntityByKey(Class<T> entityClass, Long entityId) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference, IllegalAccessException, DiscrepancyMappingColumns, InstantiationException, WrongListReference, NoSuchMethodException, InvocationTargetException, WrongJoinLeftReference {
        if (entityId > 0){
            if (OrmTableWorker.isCashed(entityClass)) {
                T result = findCashedEntity(entityClass, entityId);
                if (result != null)
                    return result;
            }
            List<T> result = getEntities(entityClass, String.format("%s = ?", getPrimaryKeyColumn(entityClass).getName()), new String[]{entityId.toString()}, null, true);
            if (result.size() > 0)
                return result.get(0);
        }
        return null;
    }

    private static Object getValue(Cursor cursor, int cursorIndex, OrmField field) {
        Class<?> type = field.getType();
        if (type.isArray()) {
            if (type.getComponentType().isPrimitive()) {
                String[] items;
                String itemsStr = cursor.getString(cursorIndex);
                if(!itemsStr.equals("")) {
                    items = itemsStr.split(",");
                } else {
                    items = new String[0];
                }
                Object result = null;
                String componentName = type.getComponentType().getSimpleName().toUpperCase();
                if (componentName.equals("INT"))
                    result = new int[items.length];
                for (int i = 0; i < items.length; i++) {
                    if (componentName.equals("INT"))
                        ((int[])result)[i] = Integer.parseInt(items[i].trim());
                }
                return result;
            }
        } else {
            String fieldType = type.getSimpleName().toUpperCase();
            if (fieldType.equals("INT"))
                return cursor.getInt(cursorIndex);
            else if (fieldType.equals("LONG"))
                return cursor.getLong(cursorIndex);
            else if (fieldType.equals("DATE"))
                return new Date(cursor.getLong(cursorIndex));
            else if (fieldType.equals("BOOLEAN"))
                return cursor.getInt(cursorIndex) == 1;
            else if (fieldType.equals("STRING"))
                return cursor.getString(cursorIndex);
            else if (fieldType.equals("DOUBLE")) {
                String value = cursor.getString(cursorIndex);
                if (value != null)
                    return Double.valueOf(value);
            } else if (fieldType.equals("DRAWABLE")) {
                byte[] bytes = cursor.getBlob(cursorIndex);
                return new BitmapDrawable(null, BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            } else if (fieldType.equals("DOCUMENT"))
                return getDocumentFromString(cursor.getString(cursorIndex));
        }
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

    private static void InsertDefaultValues(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference, IllegalAccessException {
        List<OrmEntity> valueList = new ArrayList<OrmEntity>();
        OrmFactory.getHelper().getDefaultValues(entityClass, valueList);
        if (valueList.size() > 0) {
            for (OrmEntity values : valueList) {
                values.alter();
            }
        }
    }

    private static List<DbColumn> getColumnsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongListReference, NotFindPrimaryKeyField, WrongRightJoinReference {
        ArrayList<DbColumn> columns = new ArrayList<DbColumn>();
        for(OrmField field : OrmFieldWorker.getAnnotationFieldsWithOutPrimaryKey(entityClass))
            columns.add(new DbColumn(field));
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

    static List<DbColumn> getAllColumn(Class<? extends  OrmEntity> entityClass) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference, WrongListReference {
        ArrayList<DbColumn> result = new ArrayList<DbColumn>();
        for(OrmField field : OrmFieldWorker.getAllAnnotationFields(entityClass))
            result.add(new DbColumn(field));
        return result;
    }

    private static DbColumn getPrimaryKeyColumn(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference {
        OrmField primaryField = OrmFieldWorker.getPrimaryKeyField(entityClass);
        return new DbColumn(primaryField);
    }

    static <T> List<Class<? extends OrmEntity>> getTypeArguments(Class<T> baseClass, Class<? extends T> childClass) {
        Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
        Type type = childClass;
        while (!getClass(type).equals(baseClass)) {
            if (type instanceof Class) {
                type = ((Class) type).getGenericSuperclass();
            } else {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class<?> rawType = (Class) parameterizedType.getRawType();
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
                }
                if (!rawType.equals(baseClass)) {
                    type = rawType.getGenericSuperclass();
                }
            }
        }
        Type[] actualTypeArguments;
        if (type instanceof Class) {
            actualTypeArguments = ((Class) type).getTypeParameters();
        } else {
            actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
        }
        List<Class<? extends OrmEntity>> typeArgumentsAsClasses = new ArrayList<Class<? extends OrmEntity>>();
        for (Type baseType : actualTypeArguments) {
            while (resolvedTypes.containsKey(baseType)) {
                baseType = resolvedTypes.get(baseType);
            }
            typeArgumentsAsClasses.add(getClass(baseType));
        }
        return typeArgumentsAsClasses;
    }

    private static Class<? extends OrmEntity> getClass(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type)
                    .getGenericComponentType();
            Class<? extends OrmEntity> componentClass = getClass(componentType);
            if (componentClass != null) {
                return (Class<? extends OrmEntity>) Array.newInstance(componentClass, 0).getClass();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
