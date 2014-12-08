package com.ucasoft.orm.exceptions;

import com.ucasoft.orm.OrmEntity;
import com.ucasoft.orm.OrmField;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 01.07.13
 * Time: 12:33
 */
public class DiscrepancyMappingColumns extends Exception {
    public DiscrepancyMappingColumns(Class<? extends OrmEntity> entityClass, OrmField field) {
        super(String.format("Field %s in class %s not found in relational model! Maybe you need to update the database.", field.getName(), entityClass.getSimpleName()));
    }
}
