package com.ucasoft.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA.
 * User: UCASoft
 * Date: 30.03.13
 * Time: 22:57
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {

    public String name() default "";

    public boolean primaryKey() default false;

    public boolean inherited() default false;
}
