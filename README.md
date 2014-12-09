# UcaOrm

This is new generation orm for Android.

## Get start

### 1. Include library

* Put the JAR in the **libs** subfolder of your Android project

### 2. Creating application classes

#### 2.1 Creating support classes

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
    public void getDefaultValues(Class<? extends OrmEntity> entityClass, List<OrmEntity> valueList) {

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
}
```

#### 2.2 Creating object model

``` java
    public class BaseEntity extends OrmEntity {

        @Column(primaryKey = true, inherited = true)
        private Long id;
    }

    @Table(name = "car_type", cashedList = true)
    public class CarType extends BaseEntity  {

        @Column
        private String code;

        public CarType(String code) {
            this.code = code;
        }
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
        <meta-data android:name="UO_DB_NAME" android:value="Cars" />
        <meta-data android:name="UO_DB_VERSION" android:value="1" />
    </application>
</manifest>
```

### 4. Working with ORM

#### 4.1 Creating tables

Add code to **onCreate** method in DataBaseHelper

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

#### 4.2 Adding default values

Add code to **getDefaultValues** method in DataBaseHelper

``` java
    public void getDefaultValues(Class<? extends OrmEntity> entityClass, List<OrmEntity> valueList) {
        if (entityClass.equals(CarType.class)) {
            valueList.add(new CarType("Passenger"));
            valueList.add(new CarType("Truck"));
        }
    }
```

#### 4.3 Selecting all entity instances

Add **getAllCarTypes** method in CarType

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

#### 4.4 Creating, inserting and updating instance

##### 4.4.1 Creating Car instance

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

##### 4.4.2 Updating Car instance

###### Changed Car data

``` java
    car.setEnginePower(120);
    car.getWheels().get(0).setManufacturer("Pirlin");
```

###### Update

And now also

``` java
    car.alter();
```

##### 4.4.3 Create or Update many instance

If you have list of entities, you can insert/update them in one transaction.

``` java
    OrmTransaction.WorkInTransaction(new OrmTransaction.Worker() {
        @Override
        public boolean work() {
            boolean success = true;
            for (Car car : cars){
                try {
                    if (!OrmEntity.alterInTransaction(car))
                    {
                        success = false;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    success = false;
                }
            }
            return success;
        }
    });
```

##### 4.4.4 Creating Track instance

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

#### 4.5 Selecting instances with and without child instances

##### Selecting with child instances

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

##### Selecting without child instances

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

#### 4.6 Selecting with condition

Create in Car Where method

``` java
    public static OrmWhere Where(){
        return Where(Car.class);
    }
```

Will select all cars where engine power is equals **120**

``` java
    List<Car> cars = Car.Where().Equals("engine_power", 120).Select();
```

Will select all cars where engine power is equals **120** and doors count is equals **4**

``` java
    List<Car> cars = Car.Where().Equals("engine_power", 120).And().Equals("doors_count", 4).Select();
```

Will select one car who have **Pirlin** wheel

``` java
    Car car = Car.Where().FindChild(Wheel.class, new OrmWhere(Wheel.class).Equals("manufacturer", "Pirlin")).SelectFirst();
```

#### 4.7 Updating model

Add to Car class new field **maxSpeed** and remove field **doorsCount**

``` java
    @Table(rightJoinTo = {Truck.class})
    public class Car extends BaseEntity {

        @Column(name = "car_type")
        private CarType type;

        @Column
        private List<Wheel> wheels;

        @Column(name = "engine_power")
        private int enginePower;

        @Column(name = "max_speed")
        private int maxSpeed;
    }
```

Change database version in the manifest to 2

``` xml
<manifest>
    <application android:name=".MyApplication">
    ...
        <meta-data android:name="UO_DB_NAME" android:value="Cars" />
        <meta-data android:name="UO_DB_VERSION" android:value="2" />
    </application>
</manifest>
```

Add code to **onUpdate** method in DataBaseHelper. You can write once this:

``` java
    protected void onUpgrade(int oldVersion, int newVersion) {
        try {
            if (oldVersion < 2) {
                OrmUtils.UpdateTable(Car.class).work();
            }
        } catch (Exception e) {
             e.printStackTrace();
        }
    }
```

But! You can help orm do his work! And also, if need to add to new field default value:

``` java
    protected void onUpgrade(int oldVersion, int newVersion) {
        try {
            if (oldVersion < 2) {
                OrmUtils.UpdateTable(Car.class).addColumn("max_speed", 100);
                /*Or if not need default value, just
                OrmUtils.UpdateTable(Car.class).addColumn("max_speed");*/
                OrmUtils.UpdateTable(Car.class).removeColumn("doors_count");
            }
        } catch (Exception e) {
             e.printStackTrace();
        }
    }
```
