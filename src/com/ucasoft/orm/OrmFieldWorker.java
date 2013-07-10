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
public class OrmFieldWorker {

    static class ClassFieldsInfo {

        Field primaryKey;
        List<Field> fieldsWithoutKey;
        List<Field> foreignFields;
    }

    private static HashMap<Class<? extends OrmEntity>, ClassFieldsInfo> hashedClassesInfo = new HashMap<Class<? extends OrmEntity>, ClassFieldsInfo>();

    static Field getPrimaryKeyField(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongRightJoinReference {
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

    static List<Field> getAnnotationFieldsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass) throws WrongListReference, WrongRightJoinReference, NotFindTableAnnotation {
        if (!hashedClassesInfo.containsKey(entityClass)){
            ClassFieldsInfo fieldsInfo = new ClassFieldsInfo();
            fieldsInfo.fieldsWithoutKey = getClassAnnotationFieldsWithOutPrimaryKey(entityClass, OrmTableWorker.getTableJoinLeftClass(entityClass), new ArrayList<Field>(), 0);
            hashedClassesInfo.put(entityClass, fieldsInfo);
        } else {
            ClassFieldsInfo fieldsInfo = hashedClassesInfo.get(entityClass);
            if (fieldsInfo.fieldsWithoutKey == null)
                fieldsInfo.fieldsWithoutKey = getClassAnnotationFieldsWithOutPrimaryKey(entityClass, OrmTableWorker.getTableJoinLeftClass(entityClass), new ArrayList<Field>(), 0);
        }
        return hashedClassesInfo.get(entityClass).fieldsWithoutKey;
    }

    private static List<Field> getClassAnnotationFieldsWithOutPrimaryKey(Class<? extends OrmEntity> entityClass, Class<? extends OrmEntity> jointTo, List<Field> allFields, int level) throws WrongListReference {
        ArrayList<Field> classFields = new ArrayList<Field>();
        if (!entityClass.equals(jointTo)){
            for (Field field : entityClass.getDeclaredFields()){
                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null && (level == 0 || columnAnnotation.inherited()) && !columnAnnotation.primaryKey()){
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
        else  {
            classFields.addAll(allFields);
            return classFields;
        }
    }

    static List<Field> getForeignFields(Class<? extends OrmEntity> entityClass) throws WrongListReference {
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

    private static List<Field> getForeign(Class<? extends OrmEntity> entityClass) throws WrongListReference {
        ArrayList<Field> result = new ArrayList<Field>();
        for (Field field : entityClass.getDeclaredFields()){
            if (checkForeign(entityClass, field))
                result.add(field);
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

    static List<Field> getAllAnnotationFields(Class<? extends OrmEntity> entityClass) throws WrongRightJoinReference, NotFindTableAnnotation, WrongListReference {
        ArrayList<Field> result = new ArrayList<Field>();
        result.add(getPrimaryKeyField(entityClass));
        result.addAll(getAnnotationFieldsWithOutPrimaryKey(entityClass));
        return result;
    }
}
