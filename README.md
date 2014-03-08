# UcaOrm

This is new generation orm for Android.

## Get start

### 1. Include library

* Put the JAR in the **libs** subfolder of your Android project

### 2. Create application classes

''' java
public class DataBaseHelper extends OrmHelper {

    public DataBaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    protected void onCreate() {
    }

    @Override
    protected void onUpgrade(int oldVersion, int newVersion) {

    }

    @Override
    public void getDefaultValues(Class<? extends OrmEntity> entityClass, ArrayList<String> columns, ArrayList<ContentValues> valueList) {

    }
}
'''

### 3. Android manifest
