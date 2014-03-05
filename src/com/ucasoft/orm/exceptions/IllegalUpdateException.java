package com.ucasoft.orm.exceptions;

/**
 * Created by UCASoft.
 * User: Antonov Sergey
 * Date: 05.03.14
 * Time: 10:12
 */
public class IllegalUpdateException extends Exception {

    public IllegalUpdateException() {
        super("For update entity must have not null id!");
    }
}
