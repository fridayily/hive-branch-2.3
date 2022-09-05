package org.apache.hadoop.hive.serde2.objectinspector.primitive;

import junit.framework.TestCase;
import org.apache.hadoop.hive.serde2.io.DoubleWritable;
import org.junit.Test;

import java.io.*;
/*
 * add by zhangyan
 * */

public class TestWritableDoubleObjectInspector extends TestCase {

    @Test
    public void testDoubleObjectInspector() {
        WritableDoubleObjectInspector writableDoubleObjectInspector = new WritableDoubleObjectInspector();
//        DoubleWritable doubleWritable = new DoubleWritable();
        DoubleWritable doubleWritable = new DoubleWritable();
        doubleWritable.set(12.3);
        double v1 = doubleWritable.get();
        System.out.println("doubleWritable get() " + v1);

        writableDoubleObjectInspector.set(doubleWritable, 12.1);
        double v = writableDoubleObjectInspector.get(doubleWritable);
        System.out.println("writableDoubleObjectInspector get() " + v);

        System.out.println("------------");
        System.out.println("writableDoubleObjectInspector  doubleWritable " + doubleWritable);


        Object primitiveWritableObject = writableDoubleObjectInspector.getPrimitiveWritableObject(doubleWritable);
        System.out.println("writableDoubleObjectInspector  doubleWritable " + doubleWritable);
        System.out.println("writableDoubleObjectInspector  getPrimitiveWritableObject " + primitiveWritableObject);
        System.out.println("writableDoubleObjectInspector  getPrimitiveWritableObject to string " + primitiveWritableObject.toString());

        System.out.println("o " + doubleWritable.hashCode());
        // copy doubleWritable
        Object o = writableDoubleObjectInspector.copyObject(doubleWritable);
        System.out.println("o " + o);
        System.out.println("o " + o.getClass());
        System.out.println("o hashcode " + o.hashCode());
        System.out.println("o hashcode " + o.toString());
    }

    @Test
    public void testDoubleWritable() throws IOException {
        DoubleWritable doubleWritable = new DoubleWritable();
        doubleWritable.set(12.3);
        double v1 = doubleWritable.get();
        System.out.println("doubleWritable get() " + v1);


        // 创建byte 输出 流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        doubleWritable.write(out);
        out.close();


        DoubleWritable inDoubleWritable = new DoubleWritable();

        // 创建byte 输入流
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        // 转换成DataInput流
        DataInputStream in = new DataInputStream(bais);

        // 读取字段
        inDoubleWritable.readFields(in);


        double v2 = inDoubleWritable.get();
        System.out.println("v2 " + v2);


    }


}