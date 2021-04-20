package cn.thens.okbinder;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public interface OkBinderFactory {
    Binder newBinder(Object remoteObject, Class<?> serviceClass);

    Object newProxy(IBinder binder, Class<?> serviceClass);

    interface Function {
        Object invoke(Object obj, Object[] args) throws Throwable;
    }

    class BaseBinder extends Binder implements IInterface {
        private static final String TAG = "@BaseBinder";
        protected final Object remoteObject;
        protected final String descriptor;
        protected final ClassLoader classLoader;
        private final Map<String, Function> functions = new HashMap<>();

        public BaseBinder(final Object remoteObject, Class<?> serviceClass) {
            this.remoteObject = remoteObject;
            this.descriptor = serviceClass.getName();
            this.classLoader = remoteObject.getClass().getClassLoader();
            attachInterface(this, descriptor);
            ObjectFunctions.inject(this);
        }

        protected void addFunction(String methodId, Function func) {
            functions.put(methodId, func);
        }

        public Object getRemoteObject() {
            return remoteObject;
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
                    String methodId = data.readString();
                    int argCount = data.readInt();
                    Object[] args = new Object[argCount];
                    for (int i = 0; i < argCount; i++) {
                        args[i] = OkBinderParcel.readValue(data, classLoader);
                    }
                    Function func = functions.get(methodId);
                    if (func == null) {
                        throw new NullPointerException("Func not found, " + methodId);
                    }
                    Object result = func.invoke(remoteObject, args);
                    if (reply != null) {
                        reply.writeNoException();
                        if (result != null) {
                            reply.writeInt(1);
                            OkBinderParcel.writeValue(reply, result);
                        } else {
                            reply.writeInt(0);
                        }
                    }
                    return true;
                } catch (Throwable e) {
                    Throwable cause = ErrorUtils.unwrap(e);
                    Log.e(TAG, "onTransact error", cause);
                    RemoteException remoteException = new RemoteException();
                    remoteException.initCause(cause);
                    throw remoteException;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    class BaseProxy {
        protected final IBinder binder;
        protected final ClassLoader classLoader;
        protected final String descriptor;

        public BaseProxy(IBinder binder, ClassLoader classLoader, String descriptor) {
            this.binder = binder;
            this.classLoader = classLoader;
            this.descriptor = descriptor;
        }

        protected Object transact(int flags, String methodId, Object... args) {
            if (!binder.isBinderAlive()) {
                throw new IllegalStateException("binder has died");
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            Object result = null;
            try {
                data.writeInterfaceToken(descriptor);
                data.writeString(methodId);
                if (args != null) {
                    data.writeInt(args.length);
                    for (Object arg : args) {
                        OkBinderParcel.writeValue(data, arg);
                    }
                } else {
                    data.writeInt(0);
                }
                binder.transact(Binder.FIRST_CALL_TRANSACTION, data, reply, flags);
                reply.readException();
                if (reply.readInt() != 0) {
                    result = OkBinderParcel.readValue(reply, classLoader);
                }
            } catch (Throwable e) {
                throw ErrorUtils.wrap(e);
            } finally {
                reply.recycle();
                data.recycle();
            }
            return result;
        }

        @Override
        public int hashCode() {
            return ObjectFunctions.hashCode(this);
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object obj) {
            return ObjectFunctions.equals(this, obj);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public String toString() {
            return ObjectFunctions.toString(this);
        }
    }
}
