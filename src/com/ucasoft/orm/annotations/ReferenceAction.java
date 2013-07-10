package com.ucasoft.orm.annotations;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date: 03.07.13
 * Time: 16:36
 */
public enum ReferenceAction {

    NoAction("NO ACTION"),
    Restrict("RESTRICT"),
    SetNull("SET NULL"),
    SetDefault("SET DEFAULT"),
    Cascade("CASCADE");

    private String action;

    private ReferenceAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
