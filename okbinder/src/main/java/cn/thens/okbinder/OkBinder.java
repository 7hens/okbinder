package cn.thens.okbinder;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"NullableProblems", "ConstantConditions", "unchecked"})
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
        return new MyBinder(okBinderInterface, remoteObject);
    }

    public static <T> T proxy(final Class<T> serviceClass, final IBinder binder) {
        require(isOkBinderInterface(serviceClass),
                "service class must be an interface with @OkBinder.Interface annotation");
        if (binder instanceof OkBinder.MyBinder) return ((MyBinder<T>) binder).remoteObject;
        final ClassLoader classLoader = serviceClass.getClassLoader();
        return serviceClass.cast(Proxy.newProxyInstance(classLoader, new Class[]{serviceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                require(binder.isBinderAlive(), "binder has died");
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                Object result = null;
                try {
                    data.writeInterfaceToken(serviceClass.getName());
                    data.writeString(getMethodId(method));
                    if (args != null) {
                        for (Object arg : args) {
                            MyParcel.writeValue(data, arg);
                        }
                    }
                    int flags = method.getReturnType() == Void.TYPE ? IBinder.FLAG_ONEWAY : 0;
                    binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, flags);
                    reply.readException();
                    if (reply.readInt() != 0) {
                        result = MyParcel.readValue(reply, classLoader);
                    }
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }
        }));
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalArgumentException(message);
    }

    private static boolean isOkBinderInterface(Class<?> cls) {
        return cls.isInterface() && cls.isAnnotationPresent(Interface.class);
    }

    private static Class<?> getOkBinderInterface(Object object) {
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

    private static String getMethodId(Method method) {
        StringBuilder methodSignatureBuilder = new StringBuilder()
                .append(method.getDeclaringClass().getName()).append(".").append(method.getName());
        for (Class<?> parameterType : method.getParameterTypes()) {
            methodSignatureBuilder.append(",").append(parameterType.getName());
        }
        String methodSignature = methodSignatureBuilder.toString();
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(methodSignature.getBytes());
            return Base64.encodeToString(md5, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return methodSignature;
        }
    }

    private static final class MyBinder<T> extends Binder implements IInterface {
        private final T remoteObject;
        private final String descriptor;
        private final Map<String, Method> remoteMethods = new ConcurrentHashMap<>();

        private MyBinder(Class<T> serviceClass, T remoteObject) {
            this.remoteObject = remoteObject;
            descriptor = serviceClass.getName();
            attachInterface(this, descriptor);
            for (Method method : serviceClass.getMethods()) {
                if (method.isBridge()) continue;
                remoteMethods.put(getMethodId(method), method);
            }
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == IBinder.FIRST_CALL_TRANSACTION) {
                try {
                    data.enforceInterface(descriptor);
                    Method remoteMethod = remoteMethods.get(data.readString());
                    Class<?>[] paramTypes = remoteMethod.getParameterTypes();
                    Object result;
                    if (paramTypes == null || paramTypes.length == 0) {
                        result = remoteMethod.invoke(remoteObject);
                    } else {
                        ClassLoader classLoader = remoteObject.getClass().getClassLoader();
                        Object[] args = new Object[paramTypes.length];
                        for (int i = 0; i < paramTypes.length; i++) {
                            args[i] = MyParcel.readValue(data, classLoader);
                        }
                        result = remoteMethod.invoke(remoteObject, args);
                    }
                    if (reply != null) {
                        reply.writeNoException();
                        if (result != null) {
                            reply.writeInt(1);
                            MyParcel.writeValue(reply, result);
                        } else {
                            reply.writeInt(0);
                        }
                    }
                    return true;
                } catch (Throwable e) {
                    boolean isInvokeError = e instanceof InvocationTargetException && e.getCause() != null;
                    Throwable cause = isInvokeError ? e.getCause() : e;
                    Log.e(TAG, Log.getStackTraceString(cause));
                    RemoteException remoteException = new RemoteException();
                    remoteException.initCause(cause);
                    throw remoteException;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    private final static class MyParcel {
        private static final int VAL_DEFAULT = 1;
        private static final int VAL_LIST = 2;
        private static final int VAL_SPARSEARRAY = 3;
        private static final int VAL_MAP = 4;
        private static final int VAL_OKBINDER = 5;

        static void writeValue(Parcel parcel, Object v) {
            if (v == null) {
                parcel.writeInt(VAL_DEFAULT);
                parcel.writeValue(null);
            } else if (v instanceof List) {
                parcel.writeInt(VAL_LIST);
                List val = (List) v;
                int size = val.size();
                parcel.writeInt(size);
                for (int i = 0; i < size; i++) {
                    writeValue(parcel, val.get(i));
                }
            } else if (v instanceof SparseArray) {
                parcel.writeInt(VAL_SPARSEARRAY);
                SparseArray val = (SparseArray) v;
                int size = val.size();
                parcel.writeInt(size);
                int i = 0;
                while (i < size) {
                    parcel.writeInt(val.keyAt(i));
                    parcel.writeValue(val.valueAt(i));
                    i++;
                }
            } else if (v instanceof Map) {
                parcel.writeInt(VAL_MAP);
                Map val = (Map) v;
                Set<Map.Entry<Object, Object>> entries = val.entrySet();
                parcel.writeInt(entries.size());
                for (Map.Entry<Object, Object> e : entries) {
                    writeValue(parcel, e.getKey());
                    writeValue(parcel, e.getValue());
                }
            } else {
                Class<?> okBinderInterface = getOkBinderInterface(v);
                if (okBinderInterface != null) {
                    parcel.writeInt(VAL_OKBINDER);
                    parcel.writeString(okBinderInterface.getName());
                    parcel.writeValue(new MyBinder(okBinderInterface, v));
                    return;
                }
                parcel.writeInt(VAL_DEFAULT);
                parcel.writeValue(v);
            }
        }

        static Object readValue(Parcel parcel, ClassLoader loader) throws ClassNotFoundException {
            int type = parcel.readInt();
            switch (type) {
                case VAL_LIST: {
                    List outVal = new ArrayList();
                    for (int size = parcel.readInt(); size >= 0; size--) {
                        outVal.add(readValue(parcel, loader));
                    }
                    return outVal;
                }
                case VAL_SPARSEARRAY: {
                    SparseArray outVal = new SparseArray<>();
                    for (int size = parcel.readInt(); size >= 0; size--) {
                        int key = parcel.readInt();
                        Object value = readValue(parcel, loader);
                        outVal.put(key, value);
                    }
                    return outVal;
                }
                case VAL_MAP: {
                    Map outVal = new HashMap();
                    for (int size = parcel.readInt(); size >= 0; size--) {
                        Object key = readValue(parcel, loader);
                        Object value = readValue(parcel, loader);
                        outVal.put(key, value);
                    }
                    return outVal;
                }
                case VAL_OKBINDER: {
                    Class<?> serviceClass = loader.loadClass(parcel.readString());
                    IBinder binder = (IBinder) parcel.readValue(loader);
                    return proxy(serviceClass, binder);
                }
                default:
                    return parcel.readValue(loader);
            }
        }

    }
}
