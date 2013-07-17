package com.ucasoft.orm;

import com.ucasoft.orm.exceptions.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 11.07.13
 * Time: 16:19
 */
public class OrmWhere {

    private final Class<? extends OrmEntity> entityClass;
    private String where;
    private ArrayList<String> params;

    public OrmWhere(Class<? extends OrmEntity> entityClass) {
        this.entityClass = entityClass;
        where = "";
        params = new ArrayList<String>();
    }

    public OrmWhere Equals(String column, Object value){
        where += String.format("%s = ?", column);
        params.add(value.toString());
        return this;
    }

    private <T extends OrmEntity> List<T> Select(Class<T> entityClass) throws NotFindTableAnnotation, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, IllegalAccessException, WrongListReference, NotFindPrimaryKeyField, DiscrepancyMappingColumns {
        return OrmUtils.getEntitiesWhere(entityClass, where, params.toArray(new String[params.size()]));
    }

    private <T extends OrmEntity> T SelectFirst(Class<T> entityClass) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, NotFindTableAnnotation, WrongListReference, NotFindPrimaryKeyField, DiscrepancyMappingColumns {
        return Select(entityClass).get(0);
    }

    public <T extends OrmEntity> T SelectFirst() throws NotFindTableAnnotation, InstantiationException, InvocationTargetException, NoSuchMethodException, WrongRightJoinReference, IllegalAccessException, WrongListReference, NotFindPrimaryKeyField, DiscrepancyMappingColumns {
        return (T) SelectFirst(entityClass);
    }
}
