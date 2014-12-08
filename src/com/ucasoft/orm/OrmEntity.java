package com.ucasoft.orm;

import com.ucasoft.orm.exceptions.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by UCASoft.
 * User: Antonov Sergey
 * Date: 27.06.13
 * Time: 9:22
 */
public abstract class OrmEntity {

    protected static <T extends OrmEntity> List<T> getAllEntities(Class<T> entityClass) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, NotFindTableAnnotation, DiscrepancyMappingColumns, NotFindPrimaryKeyField, WrongListReference, WrongJoinLeftReference {
        return getAllEntities(entityClass, false);
    }

    protected static <T extends OrmEntity> List<T> getAllEntities(Class<T> entityClass, boolean includeLeftChild) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, NotFindTableAnnotation, DiscrepancyMappingColumns, NotFindPrimaryKeyField, WrongListReference, WrongJoinLeftReference {
        return OrmUtils.getAllEntities(entityClass, includeLeftChild);
    }

    protected static OrmWhere Where(Class<? extends OrmEntity> entityClass){
        return new OrmWhere(entityClass);
    }

    public boolean alter() throws WrongListReference, WrongRightJoinReference, IllegalAccessException, NotFindTableAnnotation {
        return OrmUtils.alter(this);
    }

    protected boolean delete() throws IllegalAccessException, WrongRightJoinReference, NotFindPrimaryKeyField, NotFindTableAnnotation {
        return OrmUtils.delete(this);
    }
}
