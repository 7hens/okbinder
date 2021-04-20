package cn.thens.okbinder;

import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"ConstantConditions", "unchecked"})
public final class OkBinder {
    private static final String TAG = "@OkBinder";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Interface {
    }

    public static Binder create(Object remoteObject) {
        Class<?> okBinderInterface = getOkBinderInterface(remoteObject);
        require(okBinderInterface != null,
                "remote object must implement only one interface with @OkBinder.Interface annotation");
        return create(remoteObject, (Class<Object>) okBinderInterface);
    }

    public static <T> Binder create(T remoteObject, Class<T> okBinderInterface) {
        return getFactory(okBinderInterface).newBinder(remoteObject, okBinderInterface);
    }

    public static <T> T proxy(IBinder binder, Class<T> serviceClass) {
        return (T) getFactory(serviceClass).newProxy(binder, serviceClass);
    }

    private static final Map<Class<?>, OkBinderFactory> factories = new HashMap<>();
    private static final OkBinderFactory defaultFactory = new ReflectionFactory();

    private static OkBinderFactory getFactory(Class<?> serviceClass) {
        require(isOkBinderInterface(serviceClass),
                "service class must be an interface with @OkBinder.Interface annotation");
        OkBinderFactory factory = factories.get(serviceClass);
        if (factory != null) {
            return factory;
        }
        String factoryClassName = serviceClass.getName() + "_OkBinderFactory";
        try {
            ClassLoader classLoader = serviceClass.getClassLoader();
            Class<?> factoryClass = classLoader.loadClass(factoryClassName);
            factory = (OkBinderFactory) factoryClass.newInstance();
        } catch (Throwable e) {
            Log.w(TAG, "load factory class '" + factoryClassName + "' failed, " +
                    "use default factory as fallback");
        }
        if (factory == null) {
            factory = defaultFactory;
        }
        factories.put(serviceClass, factory);
        return factory;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalArgumentException(message);
    }

    static boolean isOkBinderInterface(Class<?> cls) {
        return cls.isInterface() && cls.isAnnotationPresent(Interface.class);
    }

    static Class<?> getOkBinderInterface(Object object) {
        Class<?>[] interfaces = object.getClass().getInterfaces();
        List<Class<?>> okBinderInterfaces = new ArrayList<>();
        for (Class<?> anInterface : interfaces) {
            if (isOkBinderInterface(anInterface)) {
                okBinderInterfaces.add(anInterface);
            }
        }
        if (okBinderInterfaces.size() != 1) return null;
        return okBinderInterfaces.get(0);
    }

    public static String getMethodId(Method method) {
        StringBuilder methodSignatureBuilder = new StringBuilder();
        for (Class<?> parameterType : method.getParameterTypes()) {
            methodSignatureBuilder.append(",").append(parameterType.getName());
        }
        String methodSignature = methodSignatureBuilder.toString();
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(methodSignature.getBytes());
            return method.getName() + ":" + Base64.encodeToString(md5, Base64.NO_WRAP);
        } catch (Throwable e) {
            throw ErrorUtils.wrap(e);
        }
    }
}
