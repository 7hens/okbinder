package cn.thens.okbinder2;

import org.junit.Test;

public class ClassTest {
    @SuppressWarnings("ConstantConditions")
    @Test
    public void loadPrimitiveType() throws Throwable {
        ClassLoader classLoader = getClass().getClassLoader();
        String name = int.class.getName();
        System.out.println("name: " + name);
        System.out.println("forName: " + Class.forName(name));
        System.out.println("loadClass: " + classLoader.loadClass(name));
    }

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
