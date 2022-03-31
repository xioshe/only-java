package com.xioshe.only.java.base.misc;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * 关于 Unsafe 的使用
 * 在测试类中测试具体使用场景
 *
 * @author xioshe 2022-03-31
 */
public class SomethingUnsafe {

    public static Unsafe stealInstance() throws NoSuchFieldException, IllegalAccessException {
        Field instanceField = Unsafe.class.getDeclaredField("theUnsafe");
        instanceField.setAccessible(true);
        return (Unsafe) instanceField.get(null);
    }
}
