package com.ucasoft.orm.exceptions;

import com.ucasoft.orm.OrmEntity;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 27.06.13
 * Time: 17:28
 */
public class NotFindTableAnnotation extends Exception {
    public NotFindTableAnnotation(Class<? extends OrmEntity> entityClass) {
        super(String.format("Class %s need to have @Table annotation!", entityClass.getSimpleName()));
    }
}
