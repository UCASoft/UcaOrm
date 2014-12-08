package com.ucasoft.orm.exceptions;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 27.06.13
 * Time: 15:23
 */
public class NotFindPrimaryKeyField extends Exception {
    public NotFindPrimaryKeyField(Class entityClass) {
        super(String.format("In class %s not found primary key field!", entityClass.getSimpleName()));
    }
}
