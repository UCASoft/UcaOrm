package com.ucasoft.orm.exceptions;

import com.ucasoft.orm.OrmEntity;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 17.07.13
 * Time: 21:28
 */
public class WrongJoinLeftReference extends Exception {
    public WrongJoinLeftReference(Class<? extends OrmEntity> rightClass, Class<? extends OrmEntity> entityClass) {
        super(String.format("Need add %s class to leftJoin annotation class %s!", rightClass.getSimpleName(), entityClass.getSimpleName()));
    }
}
