package com.ucasoft.orm;

import com.ucasoft.orm.annotations.Column;
import com.ucasoft.orm.annotations.ReferenceAction;
import com.ucasoft.orm.exceptions.NotFindPrimaryKeyField;
import com.ucasoft.orm.exceptions.NotFindTableAnnotation;
import com.ucasoft.orm.exceptions.WrongRightJoinReference;

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

    public DbColumn(OrmField field) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference {
        this(getColumnName(field), getColumnType(field), getColumnAdditional(field));
    }

    static String getColumnName(OrmField field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        String columnName = columnAnnotation.name();
        if (columnName.equals(""))
            columnName = field.getName().toLowerCase();
        return columnName;
    }

    static String getColumnType(OrmField field) {
        Class type = field.getType();
        if (type.isArray() && type.getComponentType().isPrimitive())
            return "TEXT";
        String fieldType = type.getSimpleName().toUpperCase();
        if (fieldType.equals("INT") || fieldType.equals("LONG") || fieldType.equals("DATE") || fieldType.equals("BOOLEAN") || isReferenceField(field))
            return "INTEGER";
        if (fieldType.equals("STRING") || fieldType.equals("DOCUMENT"))
            return "TEXT";
        if (fieldType.equals("DOUBLE"))
            return "REAL";
        if (fieldType.equals("DRAWABLE"))
            return "BLOB";
        return "";
    }

    static boolean isReferenceField(OrmField field){
        return OrmEntity.class.isAssignableFrom(field.getType());
    }

    static String getColumnAdditional(OrmField field) throws NotFindTableAnnotation, NotFindPrimaryKeyField, WrongRightJoinReference {
        Class type = field.getType();
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null){
            if (isReferenceField(field)) {
                OrmField primaryKeyField = OrmFieldWorker.getPrimaryKeyField(type);
                if (primaryKeyField == null)
                    throw new NotFindPrimaryKeyField(type);
                String reference = "REFERENCES " + OrmTableWorker.getTableName(type) + "(" + getColumnName(primaryKeyField) + ")";
                if (columnAnnotation.onDelete() != ReferenceAction.NoAction)
                    reference += " ON DELETE " + columnAnnotation.onDelete().getAction();
                return reference;
            } else {
                if (columnAnnotation.primaryKey())
                    return "PRIMARY KEY ASC";
            }
        }
        return "";
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
