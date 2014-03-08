# UcaOrm

This is new generation orm for Android.

## Get start

### 1. Include library

* Put the JAR in the **libs** subfolder of your Android project

### 2. Create application classes

``` java
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
```
``` java
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        try{
            OrmFactory.SetHelper(DataBaseHelper.class, getApplicationContext());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onTerminate() {
        OrmFactory.ReleaseHelper();
        super.onTerminate();
    }
}
```

### 3. Android manifest

``` xml
<manifest>
    <application android:name=".MyApplication">
    ...
    </application>
</manifest>
```