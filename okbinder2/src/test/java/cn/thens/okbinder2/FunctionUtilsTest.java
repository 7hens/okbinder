package cn.thens.okbinder2;

import org.junit.Assert;
import org.junit.Test;

public class FunctionUtilsTest {

    @Test
    public void testGetFunctionId() throws Throwable {
        Assert.assertEquals("toString()", FunctionUtils.getFunctionId(Object.class.getMethod("toString")));
        Assert.assertEquals("hashCode()", FunctionUtils.getFunctionId(Object.class.getMethod("hashCode")));
        Assert.assertEquals("equals(java.lang.Object)", FunctionUtils.getFunctionId(Object.class.getMethod("equals", Object.class)));
    }
}