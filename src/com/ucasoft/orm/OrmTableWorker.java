package com.ucasoft.orm;

import com.ucasoft.orm.annotations.Table;
import com.ucasoft.orm.exceptions.NotFindTableAnnotation;
import com.ucasoft.orm.exceptions.WrongRightJoinReference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by UCASoft with IntelliJ IDEA.
 * User: Antonov Sergey
 * Date:
 * Time: 15:40
 */
 public class OrmTableWorker {

    static class TableInfo {

        Class<? extends OrmEntity> joinLeftClass;
    }

    private static HashMap<Class<? extends OrmEntity>, TableInfo> hashedTableInfo = new HashMap<Class<? extends OrmEntity>, TableInfo>();

    static Class<? extends OrmEntity> getTableJoinLeftClass(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongRightJoinReference {
        if (!hashedTableInfo.containsKey(entityClass)) {
            TableInfo tableInfo = new TableInfo();
            tableInfo.joinLeftClass = getJoinLeftClass(entityClass);
            hashedTableInfo.put(entityClass, tableInfo);
        } else {
            TableInfo tableInfo = hashedTableInfo.get(entityClass);
            if (tableInfo.joinLeftClass == null)
                tableInfo.joinLeftClass = getJoinLeftClass(entityClass);
        }
        return hashedTableInfo.get(entityClass).joinLeftClass;
    }

    private static Class<? extends OrmEntity> getJoinLeftClass(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation, WrongRightJoinReference {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null)
            throw new NotFindTableAnnotation(entityClass);
        Class<? extends OrmEntity> leftClass = tableAnnotation.leftJoinTo();
        if (!leftClass.equals(OrmEntity.class)) {
            tableAnnotation = leftClass.getAnnotation(Table.class);
            if (tableAnnotation == null)
                throw new NotFindTableAnnotation(leftClass);
            boolean findReference = false;
            for(Class rightClass : tableAnnotation.rightJoinTo()){
                if (rightClass.equals(entityClass)){
                    findReference = true;
                    break;
                }
            }
            if (!findReference)
                throw new WrongRightJoinReference(entityClass, leftClass);
            return leftClass;
        }
        return null;
    }

    static List<Class<? extends OrmEntity>> getRightJoinClasses(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation { //TODO Надо закешировать!!
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null)
            throw new NotFindTableAnnotation(entityClass);
        List<Class<? extends OrmEntity>> rightJoins = Arrays.asList(tableAnnotation.rightJoinTo());
        //TODO Проверку, чтобы у все классов в RightJoin в Table аннотации был entityClass в LeftJoin!!!
        return rightJoins;
    }

    static String getTableName(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            String name = tableAnnotation.name();
            if (name.equals(""))
                name = entityClass.getSimpleName().toLowerCase();
            return name;
        } else
            throw new NotFindTableAnnotation(entityClass);
    }

    static boolean isCashed(Class<? extends OrmEntity> entityClass) throws NotFindTableAnnotation {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null)
            throw new NotFindTableAnnotation(entityClass);
        return tableAnnotation.cashedList();
    }
}
