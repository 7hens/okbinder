package cn.thens.okbinder2;

import com.squareup.javapoet.TypeName;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.ExecutableElement;

final class OkBinderCompilerUtils {

    public static String getFunctionId(Method method) {
        return getFunctionId(method.getName(), Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .collect(Collectors.toList()));
    }

    public static String getFunctionId(ExecutableElement method) {
        return getFunctionId(method.getSimpleName(), method.getParameters().stream()
                .map(type -> TypeName.get(type.asType()).toString())
                .collect(Collectors.toList()));
    }

    public static String getFunctionId(CharSequence method, List<? extends CharSequence> paramTypes) {
        String params = String.join(",", paramTypes);
        String finalParams = params.length() <= 24 ? params : hash(params.getBytes());
        return method + "(" + finalParams + ")";
    }

    private static String hash(byte[] bytes) {
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(bytes);
            return new String(Base64.getEncoder().encode(md5));
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
