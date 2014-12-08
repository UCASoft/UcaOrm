package com.ucasoft.orm.annotations;

import com.ucasoft.orm.OrmEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by UCASoft.
 * User: Antonov Sergey
 * Date: 30.03.13
 * Time: 22:52
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {

    public String name() default "";

    public boolean cashedList() default false;

    Class<? extends OrmEntity> leftJoinTo() default OrmEntity.class;

    Class<? extends OrmEntity>[] rightJoinTo() default {};
}
