# UcaOrm

This is new generation orm for Android.

## Get start

### 1. Include library

* Put the JAR in the **libs** subfolder of your Android project

### 2. Create application classes

#### 2.1 Create support classes

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

#### 2.2 Create object model

``` java
    public class BaseEntity extends OrmEntity {

        @Column(primaryKey = true, inherited = true)
        private Long id;
    }

    @Table(name = "car_type", cashedList = true)
    public class CarType extends BaseEntity  {

        @Column
        private String code;
    }

    @Table(rightJoinTo = {Truck.class})
    public class Car extends BaseEntity {

        @Column(name = "car_type")
        private CarType type;

        @Column
        private List<Wheel> wheels;

        @Column(name = "engine_power")
        private int enginePower;

        @Column(name = "doors_count")
        private int doorsCount;
    }

    @Table
    public class Wheel extends BaseEntity {

        @Column(name = "car_id")
        private Car car;

        @Column
        private String manufacturer;
    }

    @Table(leftJoinTo = Car.class)
    public class Truck extends Car {

        @Column(name = "is_tipper")
        private boolean isTipper;
    }
```

### 3. Android manifest

``` xml
<manifest>
    <application android:name=".MyApplication">
    ...
    </application>
    <meta-data android:name="UO_DB_NAME" android:value="Cars" />
    <meta-data android:name="UO_DB_VERSION" android:value="1" />
</manifest>
```

### 4. Work with ORM

#### 4.1 Create tables

Add code to onCreate method in DataBaseHelper

``` java
    protected void onCreate() {
        try {
            OrmUtils.CreateTable(CarType.class);
            OrmUtils.CreateTable(Car.class);
            OrmUtils.CreateTable(Wheel.class);
            OrmUtils.CreateTable(Truck.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```

#### 4.2 Add default values

Add code to getDefaultValues method in DataBaseHelper

``` java
    public void getDefaultValues(Class<? extends OrmEntity> entityClass, ArrayList<String> columns, ArrayList<ContentValues> valueList) {
        ContentValues values;
        if (entityClass.equals(CarType.class)) {
            values = new ContentValues();
            values.put(columns.get(0), "Passenger");
            valueList.add(values);
            values = new ContentValues();
            values.put(columns.get(0), "Truck");
            valueList.add(values);
        }
    }
```

#### 4.3 Select all entity instances

Add getAllCarTypes method in CarType

``` java
    public static List<CarType> getAllCarTypes(){
        try {
            return OrmEntity.getAllEntities(CarType.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
```

#### 4.4 Create, insert and update instance

##### 4.4.1 Create Car instance

###### Create

``` java
    Car car = new Car();
    car.setType(CarType.getAllCarTypes().get(0));
    car.setEnginePower(116);
    car.setDoorsCount(4);
    Wheel whell = new Whell();
    whell.setCar(car);
    whell.setManufacturer("Michrelli");
    car.addWheel(whell);
```

###### Insert

``` java
    car.alter();
```

##### 4.4.2 Update Car instance

###### Change Car data

``` java
    car.setEnginePower(120);
    car.getWheels().get(0).setManufacturer("Pirlin");
```

###### Update

Now just

``` java
    car.alter();
```

##### 4.4.3 Create Track instance

``` java
    Truck truck = new Truck();
    truck.setType(CarType.getAllCarTypes().get(1));
    truck.setEnginePower(220);
    truck.setDoorsCount(2);
    Wheel whell = new Whell();
    whell.setCar(truck);
    whell.setManufacturer("Michrelli");
    truck.addWheel(whell);
    truck.setTipper(true);
    truck.alter();
```

#### 4.5 Select instances with and without child instances

##### Select with child instances

``` java
    public static List<Car> getAllCars() {
        try {
            return OrmEntity.getAllEntities(CarType.class, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
```

##### Select without child instances

``` java
    public static List<Car> getCarsWithoutTrucks() {
        try {
            return OrmEntity.getAllEntities(CarType.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
```

#### 4.6 Where select

Created in Car Where method

``` java
    public static OrmWhere Where(){
        return Where(Car.class);
    }
```

Will select all cars where engine power equals 120

``` java
    List<Car> cars = Car.Where().Equals("engine_power", 120).Select();
```

Will select all cars where engine power equals 120 and doors count equals 4

``` java
    List<Car> cars = Car.Where().Equals(“engine_power”, 120).And().Equals("doors_count", 4).Select();
```

Will select one car who have Pirlin wheel

``` java
    Car car = Car.Where().FindChild(Wheel.class, new OrmWhere(Wheel.class).Equals("manufacturer", "Pirlin")).SelectFirst();
```