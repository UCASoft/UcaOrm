package com.ucasoft.orm.exceptions;

import com.ucasoft.orm.OrmEntity;

import java.lang.reflect.Field;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 01.07.13
 * Time: 12:33
 */
public class DiscrepancyMappingColumns extends Exception {
    public DiscrepancyMappingColumns(Class<? extends OrmEntity> entityClass, Field field) {
        super(String.format("Field %s in class %s not find in relational model! Maybe you need to update the database.", entityClass.getSimpleName(), field.getName()));
    }
}
