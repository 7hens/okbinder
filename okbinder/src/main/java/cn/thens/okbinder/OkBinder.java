package cn.thens.okbinder;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"NullableProblems", "ConstantConditions", "unchecked"})
public final class OkBinder<T> extends Binder implements IInterface {
    private static final String TAG = "@OKBinder";
    private final T remoteObject;
    private final String descriptor;
    private final Map<String, Method> remoteMethods = new ConcurrentHashMap<>();

    private OkBinder(Class<T> serviceClass, T remoteObject) {
        require(serviceClass.isInterface(), "service class must be an interface");
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
                Type[] paramTypes = remoteMethod.getGenericParameterTypes();
                Object result;
                if (paramTypes == null || paramTypes.length == 0) {
                    result = remoteMethod.invoke(remoteObject);
                } else {
                    ClassLoader classLoader = remoteObject.getClass().getClassLoader();
                    Object[] args = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        args[i] = readValueFromParcel(data, classLoader, paramTypes[i]);
                    }
                    result = remoteMethod.invoke(remoteObject, args);
                }
                if (reply != null) {
                    reply.writeNoException();
                    if (result != null) {
                        reply.writeInt(1);
                        writeValueToParcel(reply, remoteMethod.getGenericReturnType(), result);
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

    public static <T> OkBinder<T> server(Class<T> serviceClass, T remoteObject) {
        return new OkBinder<>(serviceClass, remoteObject);
    }

    public static <T> OkBinder<T> server(T remoteObject) {
        Class<?>[] interfaces = remoteObject.getClass().getInterfaces();
        require(interfaces.length == 1, "remoteObject can only implement one interface");
        return server((Class<T>) interfaces[0], remoteObject);
    }

    public static <T> T client(OkBinder<T> okBinder) {
        return okBinder.remoteObject;
    }

    public static <T> T client(final Class<T> serviceClass, final IBinder binder) {
        require(serviceClass.isInterface(), "service class must be an interface");
        if (binder instanceof OkBinder) return ((OkBinder<T>) binder).remoteObject;
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
                    Type[] paramTypes = method.getGenericParameterTypes();
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            writeValueToParcel(data, paramTypes[i], args[i]);
                        }
                    }
                    int flags = method.getReturnType() == Void.TYPE ? IBinder.FLAG_ONEWAY : 0;
                    binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, flags);
                    reply.readException();
                    if (reply.readInt() != 0) {
                        result = readValueFromParcel(reply, classLoader, method.getGenericReturnType());
                    }
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }
        }));
    }

    private static void writeValueToParcel(Parcel parcel, Type type, Object value) {
        parcel.writeValue(value);
    }

    private static Object readValueFromParcel(Parcel parcel, ClassLoader classLoader, Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = (parameterizedType).getRawType();
            if (rawType == OkBinder.class) {
                Class serviceClass = (Class) parameterizedType.getActualTypeArguments()[0];
                return server(serviceClass, client(serviceClass, (IBinder) parcel.readValue(classLoader)));
            }
        }
        return parcel.readValue(classLoader);
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

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalArgumentException(message);
    }
}
