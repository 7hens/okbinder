package cn.thens.okbinder;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void printObjectMethodIds() {
        for (Method method : Object.class.getMethods()) {
            if (method.isBridge()) continue;
            System.out.println(method.getName() + ":" + OkBinder.getMethodId(method));
        }
    }
}