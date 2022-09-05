package org.apache.hadoop.hive.serde2.objectinspector.primitive;


import java.sql.Struct;
import java.util.*;

import org.apache.hadoop.hive.serde2.io.DoubleWritable;
import org.apache.hadoop.hive.serde2.objectinspector.*;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.io.IntWritable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestOI {

    public static void main(String[] args) {

        //测试 根据基本类型的ObjectInspector实例，获取基本类型数据
//        testPrimitiveOI();

        //测试 根据Struct类型的ObjectInspector实例，获取基本类型数据
//        testStructOI();
        //测试 根据List类型的ObjectInspector实例，获取基本类型数据
//        testListOI();
        //测试 根据Map类型的ObjectInspector实例，获取基本类型数据
//        testMapOI();
        //测试 根据Union类型的ObjectInspector实例，获取基本类型数据
        //testUnionOI();--待完善--UNION是什么类型
    }

    //测试 根据基本类型的ObjectInspector实例，获取基本类型数据
    @Test
    public void testPrimitiveOI() {

        //第一部分：全局变量初始化
        //1.基本类型工厂方法获得Java Double型的OI实例（也可以是Hive支持的Writable型）
        PrimitiveObjectInspector poi =
                PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(
                        PrimitiveCategory.DOUBLE);
        Class<?> javaPrimitiveClass = poi.getJavaPrimitiveClass();
        System.out.println("getJavaPrimitiveClass 获取原始java类 " + javaPrimitiveClass);

        PrimitiveCategory primitiveCategory = poi.getPrimitiveCategory();
        System.out.println("primitiveCategory 获取Hive基本的类型: " + primitiveCategory);

        Class<?> primitiveWritableClass = poi.getPrimitiveWritableClass();
        System.out.println("primitiveWritableClass 获取hive 基础 Writable 类型: " + primitiveWritableClass);

        PrimitiveTypeInfo typeInfo = poi.getTypeInfo();
        System.out.println("获取 typeInfo " + typeInfo);

        String typeName = poi.getTypeName();
        System.out.println("获取 typeName " + typeName);


        ObjectInspector.Category category = poi.getCategory();
        //  类别有 PRIMITIVE, LIST, MAP, STRUCT, UNION
        System.out.println("获取 category " + category);

        Class<? extends PrimitiveObjectInspector> aClass = poi.getClass();
        System.out.println("获取类型：" + aClass);

        // 强制转换为 JavaDoubleObjectInspector 类型
        JavaDoubleObjectInspector javaDoubleObjectInspector = (JavaDoubleObjectInspector) poi;

        // 测试create 方法
        Object o = javaDoubleObjectInspector.create(2);
        System.out.println("o: " + o);
        System.out.println("o: " + o.getClass());

        // 测试set 方法
        DoubleWritable obj = new DoubleWritable();
        assertEquals((double) 12, javaDoubleObjectInspector.set(obj, 12));


        // 表示整个 numeric 的长度
        int precision = poi.precision();
        System.out.println("精度：" + precision);
        int scale = poi.scale();
        // scale 表示小数部分的长度
        System.out.println("规模：" + scale);


        //第二部分：对循环读入的参数Object按全局变量OI设置的类型解析出基本类型数据值
        //2.设定参数Object 为Double值1.0
        Object p = new Double(1.2);

        //3.通过基本类型工具类，依据 poi设定的数据类型，读取参数Object中数据
        // getDouble(Object o, PrimitiveObjectInspector oi)
        // o 有可能是short,int,string 通过 PrimitiveObjectInspector 转换成 double
        double value = PrimitiveObjectInspectorUtils.getDouble(p, poi);
        //3.或者通过基本类型OI实例解析参数值
        double value1 = ((DoubleObjectInspector) poi).get(p);

        System.out.println("PrimitiveObjectInspector: " + value + " or value1: " + value1 + "intValue: ");

    }

    @Test
    public void testDoubleOI() {
        // 获取 WritableDouble 的对象检查器
        WritableDoubleObjectInspector writableDoubleObjectInspector
                = PrimitiveObjectInspectorFactory.writableDoubleObjectInspector;


        // 利用对象检查器实例的 create 方法创建 WritableDouble 对象
        Object doubleWritableObject = writableDoubleObjectInspector.create(3.14);
        // create 方法返回的是 Object ，可以转换称对应的类型
        DoubleWritable doubleWritableData = (DoubleWritable) doubleWritableObject;

        double v = doubleWritableData.get();
        System.out.println("v: " + v);
        System.out.println("toString " + doubleWritableData.toString());
        System.out.println("getClass: " + doubleWritableData.getClass());


        System.out.println("--------------------------------------");

        DoubleWritable doubleWritable = new DoubleWritable();
        Object set = writableDoubleObjectInspector.set(doubleWritable, 3.15);
        DoubleWritable setDoubleWritable = (DoubleWritable) set;

        System.out.println("setDoubleWritable.get(): " + setDoubleWritable.get());
        System.out.println("setDoubleWritable.getClass(): " + setDoubleWritable.getClass());
        System.out.println("setDoubleWritable.toString(): " + setDoubleWritable.toString());

        // 通过其他方法得到的对象 p
        Object p = new Double(1.2);
        // 通过下面的方法获得 Object 里面的值
        // java.lang.Double cannot be cast to org.apache.hadoop.io.DoubleWritable
        try {
            double value = PrimitiveObjectInspectorUtils.getDouble(p, writableDoubleObjectInspector);
        } catch (ClassCastException e) {
            System.out.println("-------- error -------------");
            System.out.println(e.getMessage());
        }

        System.out.println("-------- get doubleWritable -------------");

        double value = PrimitiveObjectInspectorUtils.getDouble(doubleWritable, writableDoubleObjectInspector);
        System.out.println("value: " + value);


    }

    @Test
    public void testIntOI() {

        WritableIntObjectInspector writableIntObjectInspector = new WritableIntObjectInspector();

        IntWritable intWritable = new IntWritable();
        Object set = writableIntObjectInspector.set(intWritable, 12);

        // get 返回的是Object, 可以强制转换成其他类型
        int i = intWritable.get();
        System.out.println("intWriteable: " + intWritable);

        double j;

        // get 返回的是Object, 可以强制转换成其他类型，这里是double 类型

        j = intWritable.get();

        System.out.println("double: " + j);

    }

    //测试 根据Struct类型的ObjectInspector实例，获取基本类型数据
    //注：Struct是对基本类型数据的封装
    @Test
    public void testStructOI() {
        //第一部分：全局变量初始化
        //1.实例化structObjectInspector
        //1.1.Struct结构中各参数Field的 类型（Java型）指定，存放list中（也可以是Hive支持的Writable型）
        ArrayList<ObjectInspector> foi = new ArrayList<ObjectInspector>();
        foi.add(PrimitiveObjectInspectorFactory.javaLongObjectInspector);
        foi.add(PrimitiveObjectInspectorFactory.javaDoubleObjectInspector);
        //1.2.Struct结构中各参数Field的 名称指定，存放list中
        ArrayList<String> fname = new ArrayList<String>();
        fname.add("count");
        fname.add("sum");
        //1.3.其它类型的工厂方法获得Struct的OI实例（StandardStructObjectInspector）
        //StandardStructObjectInspector类是StructObjectInspector接口的实现类
        //都在同一个包下：org.apache.hadoop.hive.serde2.objectinspector
        StructObjectInspector soi =
                ObjectInspectorFactory.getStandardStructObjectInspector(
                        fname, foi);

        //2.1.跟据参数名称，从StructOI中取得该参数的封装MyField实例（
        //MyField类是StructField接口的实现类
        //MyField位置：org.apache.hadoop.hive.serde2.objectinspector.StandardStructObjectInspector.MyField
        //StructField位置：org.apache.hadoop.hive.serde2.objectinspector.StructField
        StructField countField = soi.getStructFieldRef("count");
        StructField sumField = soi.getStructFieldRef("sum");
        //2.2.进一步从参数的StructField实例取得参数的基本类型OI实例
        LongObjectInspector countFieldOI =
                (LongObjectInspector) countField.getFieldObjectInspector();
        DoubleObjectInspector sumFieldOI =
                (DoubleObjectInspector) sumField.getFieldObjectInspector();

        //第二部分：对循环读入的参数Object按全局变量OI设置的类型解析出基本类型数据值
        //注：Object[] 也是 Object 的子类
        //3.设定参数Object[] 为两个Java基本类型数据
        Object[] p = new Object[2];
        p[0] = new Long(1);
        p[1] = new Double(2.0);

        //4.1.通过StructOI及指定的参数类型，将Object[]中的数据按参数类型分离出
        Object pCount = soi.getStructFieldData(p, countField);
        Object pSum = soi.getStructFieldData(p, sumField);
        //4.2.通过参数的基本类型OI解析Object参数值
        long count = countFieldOI.get(pCount);
        double sum = sumFieldOI.get(pSum);

        System.out.println("struct {count:" + count + ", sum:" + sum + "}");
    }


    @Test
    public void testStructOI2() {
        ArrayList<ObjectInspector> foi = new ArrayList<ObjectInspector>();
        foi.add(PrimitiveObjectInspectorFactory.javaLongObjectInspector);
        foi.add(PrimitiveObjectInspectorFactory.javaDoubleObjectInspector);
        ArrayList<String> fname = new ArrayList<String>();
        fname.add("countFiled");
        fname.add("sum");
        StructObjectInspector soi =
                ObjectInspectorFactory.getStandardStructObjectInspector(
                        fname, foi);


        // MyField 是 StructField 的实现类
        // protected int fieldID;
        // protected String fieldName;
        // protected ObjectInspector fieldObjectInspector;
        // protected String fieldComment;

        System.out.println(" 获取所有字段 :" + soi.getAllStructFieldRefs());

        StructField countFiled = soi.getStructFieldRef("countFiled");
        System.out.println("countFiled 字段名称:" + countFiled.getFieldName());
        System.out.println("countFiled 字段id:" + countFiled.getFieldID());
        System.out.println("countFiled 字段类型检查器:" + countFiled.getFieldObjectInspector());

        Object[] p = new Object[2];
        p[0] = new Long(1);
        p[1] = new Double(2.0);

        List<Object> structFieldsDataAsList = soi.getStructFieldsDataAsList(p);
        System.out.println("structFieldsDataAsList: " + structFieldsDataAsList);

    }

    @Test
    public void testStructOI3() {

        ArrayList<ObjectInspector> foi = new ArrayList<ObjectInspector>();
        foi.add(PrimitiveObjectInspectorFactory.javaLongObjectInspector);
        foi.add(PrimitiveObjectInspectorFactory.javaDoubleObjectInspector);

        ArrayList<String> fname = new ArrayList<String>();
        fname.add("count");
        fname.add("sum");

        StructObjectInspector soi =
                ObjectInspectorFactory.getStandardStructObjectInspector(
                        fname, foi);


        ArrayList<ObjectInspector> myFoi = new ArrayList<ObjectInspector>();
        myFoi.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);
        myFoi.add(PrimitiveObjectInspectorFactory.javaDoubleObjectInspector);
        myFoi.add(soi);

        ArrayList<String> myFname = new ArrayList<String>();
        myFname.add("mycount");
        myFname.add("mysum");
        myFname.add("internalStruct");

        StructObjectInspector mySoi =
                ObjectInspectorFactory.getStandardStructObjectInspector(
                        myFname, myFoi);


        List<? extends StructField> allStructFieldRefs = mySoi.getAllStructFieldRefs();
        System.out.println("allStructFieldRefs: " + allStructFieldRefs);

        Object[] p = new Object[3];
        p[0] = "this count";
        p[1] = new Double(20.0);

        Object[] inP = new Object[2];
        inP[0] = new Long(1);
        inP[1] = new Double(2.23);
        p[2] = inP;

        List<Object> structFieldsDataAsList = mySoi.getStructFieldsDataAsList(p);

        System.out.println("获取所有数据 structFieldsDataAsList: " + structFieldsDataAsList);

        System.out.println("获取类型 mySoi.getCategory(): " + mySoi.getCategory());


        StructField mycount = mySoi.getStructFieldRef("mycount");

        System.out.println("mycount: " + mycount);
        System.out.println("mycount.getClass(): " + mycount.getClass());
        System.out.println("mycount.getFieldName(): " + mycount.getFieldName());
        System.out.println("mycount.getFieldID(): " + mycount.getFieldID());
        System.out.println("mycount.getFieldObjectInspector(): " + mycount.getFieldObjectInspector());
        System.out.println("mycount.getFieldComment(): " + mycount.getFieldComment());


        StructField internalStructField = mySoi.getStructFieldRef("internalStruct");

        System.out.println("internalStruct: " + internalStructField);
        System.out.println("internalStruct.getClass(): " + internalStructField.getClass());
        System.out.println("internalStruct.getFieldName(): " + internalStructField.getFieldName());
        System.out.println("internalStruct.getFieldID(): " + internalStructField.getFieldID());
        System.out.println("internalStruct.getFieldObjectInspector(): " + internalStructField.getFieldObjectInspector());
        System.out.println("internalStruct.getFieldComment(): " + internalStructField.getFieldComment());


        List<Object> internalStructFieldsDataAsList = soi.getStructFieldsDataAsList(inP);
        System.out.println("internalStructFieldsDataAsList: "+ internalStructFieldsDataAsList);

        Object structFieldData = mySoi.getStructFieldData(p, internalStructField);

        System.out.println(((Object[]) structFieldData)[0]);
        System.out.println(((Object[]) structFieldData)[1]);
//        System.out.println("structFieldData: " + listFieldData.get(0));

    }

    //测试 根据List类型的ObjectInspector实例，获取基本类型数据
    //注：List是对基本类型数据的封装
    @Test
    public void testListOI() {
        //第一部分：全局变量初始化
        //1.1.实例化listObjectInspector，并指定list元素为Java Long型
        ListObjectInspector loi =
                ObjectInspectorFactory.getStandardListObjectInspector(
                        PrimitiveObjectInspectorFactory.javaLongObjectInspector);
        //1.2.取得list元素的基本类型OI实例
        //注：也可以反过来，先取得基本类型OI实例，再取得ListOI实例
        PrimitiveObjectInspector poi = (PrimitiveObjectInspector) loi
                .getListElementObjectInspector();

        //第二部分：对循环读入的参数Object按全局变量OI设置的类型解析出基本类型数据值
        //注：Object[] 也是 Object 的子类
        //2.设定参数Object[] 为三个Java基本类型数据
        Object[] p = new Object[3];
        p[0] = new Long(12);
        p[1] = new Long(13);
        p[2] = new Long(14);

        //3.通过ListOI循环分离出各元素基本类型数据
        Object o = null;
        long value;
        int listLen = loi.getListLength(p);
        System.out.print("list {");
        for (int i = 0; i < listLen; i++) {
            //3.1.通过ListOI循环分离出各元素基本类型数据
            o = loi.getListElement(p, i);
            //3.2.通过基本类型工具类，依据 poi设定的数据类型，读取参数Object中数据
            value = PrimitiveObjectInspectorUtils.getLong(o, poi);
            //3.2.或者通过基本类型OI实例解析参数值
            //value = ((LongObjectInspector) poi).get(o);
            System.out.print(value);
            if (i < listLen - 1) System.out.print(",");
        }
        System.out.println("}");

    }

    //测试 根据Map类型的ObjectInspector实例，获取基本类型数据
    //注：List是对基本类型数据的封装
    @Test
    public void testMapOI() {
        //第一部分：全局变量初始化
        //1.1.取得map键值对key/value的基本类型OI实例
        LongObjectInspector keyLongOI = (LongObjectInspector) PrimitiveObjectInspectorFactory
                .getPrimitiveJavaObjectInspector(PrimitiveCategory.LONG);
        DoubleObjectInspector valueDoubleOI = (DoubleObjectInspector) PrimitiveObjectInspectorFactory
                .getPrimitiveJavaObjectInspector(PrimitiveCategory.DOUBLE);
        //1.2.实例化mapObjectInspector
        MapObjectInspector moi =
                ObjectInspectorFactory.getStandardMapObjectInspector(
                        keyLongOI, valueDoubleOI);

        //第二部分：对循环读入的参数Object按全局变量OI设置的类型解析出基本类型数据值
        //2.设定参数Object 为两个键值对的Java Map类型数据
        Map<Long, Double> pmap = new HashMap<Long, Double>();
        pmap.put(new Long(1), new Double(1.1));
        pmap.put(new Long(2), new Double(2.2));
        Object p = pmap;

        //3.通过mapOI循环分离出键值对元素基本类型数据
        Map<Object, Object> map = (Map<Object, Object>) moi.getMap(p);
        Iterator<Object> it = map.keySet().iterator();
        Object oKey, oValue;
        long key;
        double value;
        System.out.print("map {");
        while (it.hasNext()) {
            oKey = it.next();
            oValue = map.get(oKey);
            //通过键值对基本类型OI解析参数值
            key = keyLongOI.get(oKey);
            value = valueDoubleOI.get(oValue);
            System.out.print("\"" + key + "\":" + "\"" + value + "\" ");
        }
        System.out.print("}");

    }


    @Test
    public void testMapOI2() {
        //第一部分：全局变量初始化
        //1.1.取得map键值对key/value的基本类型OI实例
        StringObjectInspector keyStringOI = (StringObjectInspector) PrimitiveObjectInspectorFactory
                .getPrimitiveJavaObjectInspector(PrimitiveCategory.STRING);
        DoubleObjectInspector valueDoubleOI = (DoubleObjectInspector) PrimitiveObjectInspectorFactory
                .getPrimitiveJavaObjectInspector(PrimitiveCategory.DOUBLE);
        //1.2.实例化mapObjectInspector
        MapObjectInspector moi =
                ObjectInspectorFactory.getStandardMapObjectInspector(
                        keyStringOI, valueDoubleOI);

        //第二部分：对循环读入的参数Object按全局变量OI设置的类型解析出基本类型数据值
        //2.设定参数Object 为两个键值对的Java Map类型数据
        Map<String, Double> pmap = new HashMap<String, Double>();
        pmap.put("a", new Double(1.1));
        pmap.put("b", new Double(2.2));
        Object p = pmap;

        //3.通过mapOI循环分离出键值对元素基本类型数据
        Map<Object, Object> map = (Map<Object, Object>) moi.getMap(p);
        Iterator<Object> it = map.keySet().iterator();
        Object oKey, oValue;
        String key;
        double value;
        System.out.print("map {");
        while (it.hasNext()) {
            oKey = it.next();
            oValue = map.get(oKey);
            //通过键值对基本类型OI解析参数值
            key = keyStringOI.getPrimitiveJavaObject(oKey);
            value = valueDoubleOI.get(oValue);
            System.out.print("\"" + key + "\":" + "\"" + value + "\" ");
        }
        System.out.print("}");

    }

}
