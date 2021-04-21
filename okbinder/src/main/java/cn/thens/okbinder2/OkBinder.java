package cn.thens.okbinder2;

import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"ConstantConditions", "unchecked"})
public final class OkBinder {
    private static final String TAG = "@OkBinder";

    public static Binder create(Object remoteObject) {
        Class<?> okBinderInterface = getOkBinderInterface(remoteObject);
        require(okBinderInterface != null,
                "remote object must implement only one interface with @AIDL annotation");
        return create((Class<Object>) okBinderInterface, remoteObject);
    }

    public static <T> Binder create(Class<T> okBinderInterface, T remoteObject) {
        return getFactory(okBinderInterface).newBinder(okBinderInterface, remoteObject);
    }

    public static <T> T proxy(Class<T> serviceClass, IBinder binder) {
        if (binder instanceof OkBinderFactory.BaseBinder) {
            return (T) ((OkBinderFactory.BaseBinder) binder).getRemoteObject();
        }
        return (T) getFactory(serviceClass).newProxy(serviceClass, binder);
    }

    private static final Map<Class<?>, OkBinderFactory> factories = new HashMap<>();
    private static final OkBinderFactory defaultFactory = new ReflectionFactory();

    private static OkBinderFactory getFactory(Class<?> serviceClass) {
        require(isOkBinderInterface(serviceClass),
                "service class must be an interface with @AIDL annotation");
        OkBinderFactory factory = factories.get(serviceClass);
        if (factory != null) {
            return factory;
        }
        String factoryClassName = serviceClass.getName() + "Factory";
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
        return cls.isInterface() && cls.isAnnotationPresent(AIDL.class);
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

    public static String getFunctionId(Method method) {
        StringBuilder functionId = new StringBuilder(method.getName());
        StringBuilder params = new StringBuilder();
        boolean isFirst = true;
        for (Class<?> paramType : method.getParameterTypes()) {
            params.append(isFirst ? "" : ",").append(paramType.getName());
            isFirst = false;
        }
        if (params.length() <= 24) {
            return functionId.append("(").append(params).append(")").toString();
        }
        try {
            byte[] bytes = params.toString().getBytes();
            byte[] md5 = MessageDigest.getInstance("MD5").digest(bytes);
            String base64 = Base64.encodeToString(md5, Base64.NO_WRAP);
            return functionId.append("(").append(base64).append(")").toString();
        } catch (Throwable e) {
            throw ErrorUtils.wrap(e);
        }
    }
}
