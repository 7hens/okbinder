package cn.thens.okbinder2;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public interface OkBinderFactory {
    Binder newBinder(Class<?> serviceClass, Object remoteObject);

    Object newProxy(Class<?> serviceClass, IBinder binder);

    interface Function {
        Object invoke(Object obj, Object[] args) throws Throwable;
    }

    class BaseBinder extends Binder implements IInterface {
        protected final Object remoteObject;
        protected final String descriptor;
        protected final ClassLoader classLoader;
        private final Map<String, Function> functions = new HashMap<>();

        public BaseBinder(Class<?> serviceClass, final Object remoteObject) {
            this.remoteObject = remoteObject;
            this.descriptor = serviceClass.getName();
            this.classLoader = remoteObject.getClass().getClassLoader();
            attachInterface(this, descriptor);
            ObjectFunctions.inject(this);
        }

        protected void register(String functionId, Function func) {
            functions.put(functionId, func);
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
                    checkMagicNumber(data.readInt());
                    checkVersion(data.readInt());
                    String functionId = data.readString();
                    int argCount = data.readInt();
                    Object[] args = new Object[argCount];
                    for (int i = 0; i < argCount; i++) {
                        args[i] = OkBinderParcel.read(data, classLoader);
                    }
                    Function function = functions.get(functionId);
                    if (function == null) {
                        throw new NoSuchElementException("Unregistered function " + functionId);
                    }
                    Object result = function.invoke(remoteObject, args);
                    if (reply != null) {
                        reply.writeNoException();
                        if (result != null) {
                            reply.writeInt(1);
                            OkBinderParcel.write(reply, result);
                        } else {
                            reply.writeInt(0);
                        }
                    }
                    return true;
                } catch (Throwable e) {
                    Throwable cause = ErrorUtils.unwrap(e);
                    Log.e(OkBinder.TAG, "Binder call failed", cause);
                    RemoteException remoteException = new RemoteException();
                    remoteException.initCause(cause);
                    throw remoteException;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private void checkMagicNumber(int magicNumber) {
            if (magicNumber != OkBinder.MAGIC_NUMBER) {
                throw new IllegalArgumentException("Mismatched magic number " + magicNumber);
            }
        }

        private void checkVersion(int version) {
            OkBinderVersion.Spec currentVersion = OkBinderVersion.current();
            if (!currentVersion.isSupported(version)) {
                throw new IllegalArgumentException("Unsupported version " + version +
                        ", current is " + currentVersion.code);
            }
        }
    }

    class BaseProxy {
        protected final IBinder binder;
        protected final ClassLoader classLoader;
        protected final String descriptor;

        public BaseProxy(Class<?> serviceClass, IBinder binder) {
            this.binder = binder;
            this.classLoader = serviceClass.getClassLoader();
            this.descriptor = serviceClass.getName();
        }

        protected Object transact(int flags, String functionId, Object... args) {
            if (!binder.isBinderAlive()) {
                throw new IllegalStateException("binder has died");
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            Object result = null;
            try {
                data.writeInterfaceToken(descriptor);
                data.writeInt(OkBinder.MAGIC_NUMBER);
                data.writeInt(OkBinderVersion.current().code);
                data.writeString(functionId);
                if (args != null) {
                    data.writeInt(args.length);
                    for (Object arg : args) {
                        OkBinderParcel.write(data, arg);
                    }
                } else {
                    data.writeInt(0);
                }
                binder.transact(Binder.FIRST_CALL_TRANSACTION, data, reply, flags);
                reply.readException();
                if (reply.readInt() != 0) {
                    result = OkBinderParcel.read(reply, classLoader);
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
