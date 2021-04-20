package cn.thens.okbinder;

import android.os.Binder;
import android.os.IBinder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class ReflectionFactory implements OkBinderFactory {
    @Override
    public Binder newBinder(Object remoteObject, Class<?> serviceClass) {
        return new MyBinder(remoteObject, serviceClass);
    }

    @Override
    public Object newProxy(final IBinder binder, final Class<?> serviceClass) {
        if (binder instanceof BaseBinder) return ((BaseBinder) binder).getRemoteObject();
        final ClassLoader classLoader = serviceClass.getClassLoader();
        return serviceClass.cast(Proxy.newProxyInstance(classLoader, new Class[]{serviceClass},
                new MyProxy(binder, classLoader, serviceClass)));
    }

    private static final class MethodFunction implements Function {
        private final Method method;

        private MethodFunction(Method method) {
            this.method = method;
        }

        @Override
        public Object invoke(Object obj, Object[] args) throws Throwable{
            return method.invoke(obj, args);
        }
    }

    private static final class MyBinder extends BaseBinder {
        private MyBinder(Object remoteObject, Class<?> serviceClass) {
            super(remoteObject, serviceClass);
            for (Method method : serviceClass.getMethods()) {
                if (method.isBridge()) continue;
                addFunction(OkBinder.getMethodId(method), new MethodFunction(method));
            }
        }
    }

    private static final class MyProxy extends BaseProxy implements InvocationHandler {
        public MyProxy(IBinder binder, ClassLoader classLoader, Class<?> serviceClass) {
            super(binder, classLoader, serviceClass.getName());
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            int flags = method.getReturnType() == Void.TYPE ? IBinder.FLAG_ONEWAY : 0;
            return transact(flags, OkBinder.getMethodId(method), args);
        }
    }
}
