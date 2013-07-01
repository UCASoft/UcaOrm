package com.ucasoft.orm;

import com.ucasoft.orm.exceptions.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 27.06.13
 * Time: 9:22
 */
public class OrmEntity {

    protected static <T extends OrmEntity> ArrayList<T> getAllEntities(Class<T> entityClass) throws IllegalAccessException, NotFindTableAnnotation, NotFindPrimaryKeyField, WrongListReference, InstantiationException, WrongRightJoinReference, NoSuchMethodException, InvocationTargetException, DiscrepancyMappingColumns {
        return OrmUtils.getAllEntities(entityClass);
    }

    public boolean insert() {
        try {
            return OrmUtils.alter(this);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
