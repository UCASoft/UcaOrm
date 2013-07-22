package com.ucasoft.orm;

/**
 * Created with IntelliJ IDEA.
 * User: UCASoft
 * Date: 30.06.13
 * Time: 13:08
 */
class DbColumn {

    private String name;

    private String type;

    private String additional;

    public DbColumn(String name, String type, String additional) {
        this.name = name;
        this.type = type;
        this.additional = additional;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getAdditional() {
        return additional;
    }

    @Override
    public String toString() {
        String result = name + " " + type;
        if (!additional.equals(""))
            result += " " + additional;
        return result;
    }
}
