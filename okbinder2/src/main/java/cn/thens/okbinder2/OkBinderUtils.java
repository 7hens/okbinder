package cn.thens.okbinder2;

import android.util.Base64;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class OkBinderUtils {

    public static String getFunctionId(Method method) {
        return getFunctionId(method.getName(), toParamTypeNames(method.getParameterTypes()));
    }

    private static List<String> toParamTypeNames(Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(parameterTypes.length);
        for (Class<?> parameterType : parameterTypes) {
            list.add(parameterType.getName());
        }
        return list;
    }

    private static String getFunctionId(CharSequence method, List<? extends CharSequence> paramTypes) {
        String params = String.join(",", paramTypes);
        String finalParams = params.length() <= 24 ? params : hash(params.getBytes());
        return method + "(" + finalParams + ")";
    }

    private static String hash(byte[] bytes) {
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(bytes);
            return Base64.encodeToString(md5, Base64.NO_WRAP);
        } catch (Throwable e) {
            throw ErrorUtils.wrap(e);
        }
    }
}
