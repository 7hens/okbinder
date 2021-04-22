package cn.thens.okbinder2;

import android.os.Binder;
import android.os.IBinder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class ReflectionFactory implements OkBinderFactory {
    @Override
    public Binder newBinder(Class<?> serviceClass, Object remoteObject) {
        return new MyBinder(remoteObject, serviceClass);
    }

    @Override
    public Object newProxy(final Class<?> serviceClass, final IBinder binder) {
        final ClassLoader classLoader = serviceClass.getClassLoader();
        return serviceClass.cast(Proxy.newProxyInstance(classLoader, new Class[]{serviceClass},
                new MyProxy(binder, serviceClass)));
    }

    private static final class MethodFunction implements Function {
        private final Method method;

        private MethodFunction(Method method) {
            this.method = method;
        }

        @Override
        public Object invoke(Object obj, Object[] args) throws Throwable {
            try {
                return method.invoke(obj, args);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw cause != null ? cause : e;
            }
        }
    }

    private static final class MyBinder extends BaseBinder {
        private MyBinder(Object remoteObject, Class<?> serviceClass) {
            super(serviceClass, remoteObject);
            for (Method method : serviceClass.getMethods()) {
                if (method.isBridge()) continue;
                register(OkBinder.getFunctionId(method), new MethodFunction(method));
            }
        }
    }

    private static final class MyProxy extends BaseProxy implements InvocationHandler {
        public MyProxy(IBinder binder, Class<?> serviceClass) {
            super(serviceClass, binder);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            int flags = isOneWay(method) ? IBinder.FLAG_ONEWAY : 0;
            return transact(flags, OkBinder.getFunctionId(method), args);
        }
    }

    private static boolean isOneWay(Method method) {
        return method.getReturnType() == Void.TYPE;
    }
}
