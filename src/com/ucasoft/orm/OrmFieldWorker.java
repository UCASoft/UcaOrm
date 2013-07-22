package com.ucasoft.orm;

import com.ucasoft.orm.annotations.Column;
import com.ucasoft.orm.exceptions.NotFindTableAnnotation;
import com.ucasoft.orm.exceptions.WrongListReference;
import com.ucasoft.orm.exceptions.WrongRightJoinReference;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 10.07.13
 * Time: 15:37
 */
class OrmFieldWorker {

    static class ClassFieldsInfo {

        OrmField primaryKey;
        List<OrmField> fieldsWithoutKey;
        List<OrmField> foreignFields;
        HashMap<Class<? extends OrmEntity>, OrmField> referenceFields;
    }

    private static HashMap<Class<? extends OrmEntity>, ClassFieldsInfo> hashedClassesInfo = new HashMap<Class<? extends OrmEntity>, ClassFieldsInfo>();

    static OrmField getPrimaryKeyField(Class<? extends OrmEntity> entityClass, boolean setAccessible) throws WrongRightJoinReference, NotFindTableAnnotation {
        OrmField field = getPrimaryKeyField(entityClass);
        if (setAccessible) // Special check true, don't do just field.setAccessible(setAccessible);
            field.setAccessible(true);
        return field;
    }

    static OrmField getPrimaryKeyField(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongRightJoinReference {
        if (!hashedClassesInfo.containsKey(entityClass)){
            ClassFieldsInfo classFieldsInfo = new ClassFieldsInfo();
            classFieldsInfo.primaryKey = getPrimaryKeyField(entityClass, OrmTableWorker.getTableJoinLeftClass(entityClass));
            hashedClassesInfo.put(entityClass, classFieldsInfo);
        } else {
            ClassFieldsInfo classFieldsInfo = hashedClassesInfo.get(entityClass);
            if (classFieldsInfo.primaryKey == null)
                classFieldsInfo.primaryKey = getPrimaryKeyField(entityClass, OrmTableWorker.getTableJoinLeftClass(entityClass));
        }
        return hashedClassesInfo.get(entityClass).primaryKey;
    }

    private static OrmField getPrimaryKeyField(Class entityClass, Class<? extends OrmEntity> joinTo) {
        if (joinTo != null){
            return new OrmField(joinTo, null);
        }
        if (!entityClass.equals(joinTo)) {
            for (Field field : entityClass.getDeclaredFields()) {
                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null && columnAnnotation.primaryKey())
                    return new OrmField(field);
            }
        }
        if (OrmEntity.class.isAssignableFrom(entityClass.getSuperclass()))
            return getPrimaryKeyField(entityClass.getSuperclass(), joinTo);
        return null;
    }

    static List<OrmField> getAnnotationFieldsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass) throws WrongListReference, WrongRightJoinReference, NotFindTableAnnotation {
        if (!hashedClassesInfo.containsKey(entityClass)){
            ClassFieldsInfo fieldsInfo = new ClassFieldsInfo();
            fieldsInfo.fieldsWithoutKey = getClassAnnotationFieldsWithOutPrimaryKey(entityClass, OrmTableWorker.getTableJoinLeftClass(entityClass), new ArrayList<OrmField>(), 0);
            hashedClassesInfo.put(entityClass, fieldsInfo);
        } else {
            ClassFieldsInfo fieldsInfo = hashedClassesInfo.get(entityClass);
            if (fieldsInfo.fieldsWithoutKey == null)
                fieldsInfo.fieldsWithoutKey = getClassAnnotationFieldsWithOutPrimaryKey(entityClass, OrmTableWorker.getTableJoinLeftClass(entityClass), new ArrayList<OrmField>(), 0);
        }
        return hashedClassesInfo.get(entityClass).fieldsWithoutKey;
    }

    private static List<OrmField> getClassAnnotationFieldsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass, Class<? extends OrmEntity> jointTo, List<OrmField> allFields, int level) throws WrongListReference {
        ArrayList<OrmField> classFields = new ArrayList<OrmField>();
        if (!entityClass.equals(jointTo)){
            for (Field field : entityClass.getDeclaredFields()){
                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null && (level == 0 || columnAnnotation.inherited()) && !columnAnnotation.primaryKey()){
                    if (!checkForeign(entityClass, field))
                        classFields.add(new OrmField(field));
            }
        }
            List<OrmField> fields = concatFields(allFields, classFields);
        Class superClass = entityClass.getSuperclass();
        if (OrmEntity.class.isAssignableFrom(superClass))
            return getClassAnnotationFieldsWithOutPrimaryKey(superClass, jointTo, fields, level + 1);
        return fields;
    }
        return allFields;
    }

    private static List<OrmField> concatFields(List<OrmField> allFields, List<OrmField> classFields) {
        if (classFields.size() == 0)
            return allFields;
        else  {
            classFields.addAll(allFields);
            return classFields;
        }
    }

    static List<OrmField> getForeignFields(Class<? extends OrmEntity> entityClass) throws WrongListReference {
        if (!hashedClassesInfo.containsKey(entityClass)){
            ClassFieldsInfo fieldsInfo = new ClassFieldsInfo();
            fieldsInfo.foreignFields = getForeign(entityClass);
            hashedClassesInfo.put(entityClass, fieldsInfo);
        } else {
            ClassFieldsInfo fieldsInfo = hashedClassesInfo.get(entityClass);
            if (fieldsInfo.foreignFields == null)
                fieldsInfo.foreignFields = getForeign(entityClass);
        }
        return hashedClassesInfo.get(entityClass).foreignFields;
    }

    private static List<OrmField> getForeign(Class<? extends OrmEntity> entityClass) throws WrongListReference {
        ArrayList<OrmField> result = new ArrayList<OrmField>();
        for (Field field : entityClass.getDeclaredFields()){
            if (checkForeign(entityClass, field))
                result.add(new OrmField(field));
        }
        return result;
    }

    private static boolean checkForeign(Class parentClass, Field field) throws WrongListReference {
        Class type = field.getType();
        if (type.getSimpleName().equals("List")) {
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            Class itemType = (Class) listType.getActualTypeArguments()[0];
            if (OrmEntity.class.isAssignableFrom(itemType)){
                boolean okReference = false;
                for (Field classField : itemType.getDeclaredFields()){
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

    static List<OrmField> getAllAnnotationFields(Class<? extends OrmEntity> entityClass) throws WrongRightJoinReference, NotFindTableAnnotation, WrongListReference {
        ArrayList<OrmField> result = new ArrayList<OrmField>();
        result.add(getPrimaryKeyField(entityClass));
        result.addAll(getAnnotationFieldsWithOutPrimaryKey(entityClass));
        return result;
    }

    static OrmField getReferenceField(Class<? extends OrmEntity> entityClass, Class<? extends OrmEntity> referenceTo) throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference {
        if (!hashedClassesInfo.containsKey(entityClass)) {
            ClassFieldsInfo classFieldsInfo = new ClassFieldsInfo();
            classFieldsInfo.referenceFields = new HashMap<Class<? extends OrmEntity>, OrmField>();
            classFieldsInfo.referenceFields.put(referenceTo, getClassReferenceField(entityClass, referenceTo));
            hashedClassesInfo.put(entityClass, classFieldsInfo);
        } else {
            ClassFieldsInfo classFieldsInfo = hashedClassesInfo.get(entityClass);
            if (classFieldsInfo.referenceFields == null) {
                classFieldsInfo.referenceFields = new HashMap<Class<? extends OrmEntity>, OrmField>();
                classFieldsInfo.referenceFields.put(referenceTo, getClassReferenceField(entityClass, referenceTo));
            } else if (!classFieldsInfo.referenceFields.containsKey(referenceTo))
                classFieldsInfo.referenceFields.put(referenceTo, getClassReferenceField(entityClass, referenceTo));
        }
        return hashedClassesInfo.get(entityClass).referenceFields.get(referenceTo);
    }

    private static OrmField getClassReferenceField(Class<? extends OrmEntity> entityClass, Class<? extends OrmEntity> referenceTo) throws NotFindTableAnnotation, WrongListReference, WrongRightJoinReference {
        for (OrmField field : getAnnotationFieldsWithOutPrimaryKey(entityClass)){
            if (referenceTo.isAssignableFrom(field.getType()))
                return field;
        }
        return null;
    }
}
