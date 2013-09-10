package com.ucasoft.orm;

import com.ucasoft.orm.annotations.Column;
import com.ucasoft.orm.annotations.ReferenceAction;
import com.ucasoft.orm.exceptions.NotFindTableAnnotation;
import com.ucasoft.orm.exceptions.WrongRightJoinReference;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 12.07.13
 * Time: 13:09
 */
public final class OrmField {

    private Field field;
    private Class<? extends OrmEntity> leftJoin;
    private Class<? extends OrmEntity> rightJoin;

    public OrmField(Field field) {
        this.field = field;
    }

    public OrmField(Class<? extends OrmEntity> leftJoin, Class<? extends OrmEntity> rightJoin) {
        this.leftJoin = leftJoin;
        this.rightJoin = rightJoin;
    }

    public void setAccessible(boolean accessible) {
        if (field != null)
            field.setAccessible(accessible);
    }

    public Object get(OrmEntity entity) throws IllegalAccessException {
        if (field != null)
            return field.get(entity);
        if (leftJoin != null){
            try {
                return OrmFieldWorker.getPrimaryKeyField(leftJoin).get(entity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void set(OrmEntity entity, Object value) throws IllegalAccessException {
        if (field != null)
            field.set(entity, value);
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        if (field != null)
            return field.getAnnotation(annotationClass);
        if (leftJoin != null)
            return (A) getLeftJoinColumn();
        return null;
    }

    private Column getLeftJoinColumn() {
        return new Column() {
            @Override
            public String name() {
                return "";
            }

            @Override
            public boolean primaryKey() {
                return false;
            }

            @Override
            public boolean inherited() {
                return false;
            }

            @Override
            public ReferenceAction onDelete() {
                return ReferenceAction.Cascade;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public boolean equals(Object o) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return null;
            }
        };
    }

    public String getName() {
        if (field != null)
            return field.getName();
        if (leftJoin != null)
        {
            try {
                return String.format("%s_%s", OrmTableWorker.getTableName(leftJoin), OrmFieldWorker.getPrimaryKeyField(leftJoin).getName().toLowerCase());
            } catch (NotFindTableAnnotation notFindTableAnnotation) {
                notFindTableAnnotation.printStackTrace();
            } catch (WrongRightJoinReference wrongRightJoinReference) {
                wrongRightJoinReference.printStackTrace();
            }
        }
        return "";
    }

    public Class<?> getType() {
        if (field != null)
            return field.getType();
        if (leftJoin != null)
            return leftJoin;
        return null;
    }

    public int getInt(OrmEntity entity) throws IllegalAccessException {
        if (field != null)
            return field.getInt(entity);
        throw new IllegalAccessException();
    }

    public Double getDouble(OrmEntity entity) throws IllegalAccessException {
        if (field != null) {
            Object value = field.get(entity);
            if (value != null)
                return Double.valueOf(value.toString());
            else
                return null;
        }
        throw new IllegalAccessException();
    }

    public Type getGenericType() {
        if (field != null)
            return field.getGenericType();
        return null;
    }
}
