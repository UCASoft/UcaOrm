package com.ucasoft.orm;

import com.ucasoft.orm.exceptions.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 27.06.13
 * Time: 9:22
 */
public class OrmEntity {

    protected static <T extends OrmEntity> List<T> getAllEntities(Class<T> entityClass) throws IllegalAccessException, NotFindTableAnnotation, NotFindPrimaryKeyField, WrongListReference, InstantiationException, WrongRightJoinReference, NoSuchMethodException, InvocationTargetException, DiscrepancyMappingColumns {
        return OrmUtils.getAllEntities(entityClass);
    }

    public boolean alter() throws WrongListReference, WrongRightJoinReference, IllegalAccessException, NotFindTableAnnotation {
            return OrmUtils.alter(this);
        }

    public boolean delete() throws IllegalAccessException, WrongRightJoinReference, NotFindPrimaryKeyField, NotFindTableAnnotation {
        return OrmUtils.delete(this);
    }
}
