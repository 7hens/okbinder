package cn.thens.okbinder2;

import org.junit.Test;

import static org.junit.Assert.*;

public class ClassTest {
    @Test
    public void testPrimitiveType() throws Throwable {
        testClass(int.class);
        testClass(int[].class);
        testClass(int[][].class);
    }

    @SuppressWarnings("ConstantConditions")
    private void testClass(Class<?> cls) throws Throwable {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            String name = cls.getName();
            System.out.println(name + " isPrimitive: " + cls.isPrimitive());
            System.out.println("- forName: " + Class.forName(name));
            System.out.println("- loadClass: " + classLoader.loadClass(name));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
