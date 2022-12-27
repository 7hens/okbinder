package cn.thens.okbinder2;

import org.junit.Assert;
import org.junit.Test;

public class OkBinderUtilsTest {

    @Test
    public void testGetFunctionId() throws Throwable {
        Assert.assertEquals("toString()", OkBinderUtils.getFunctionId(Object.class.getMethod("toString")));
        Assert.assertEquals("hashCode()", OkBinderUtils.getFunctionId(Object.class.getMethod("hashCode")));
        Assert.assertEquals("equals(java.lang.Object)", OkBinderUtils.getFunctionId(Object.class.getMethod("equals", Object.class)));
    }
}