package cn.thens.okbinder2;

final class ObjectFunctions {
    private static final String TO_STRING = "toString()";
    private static final String HASH_CODE = "hashCode()";
    private static final String EQUALS = "equals(java.lang.Object)";

    private static final OkBinderFactory.Function toString = new OkBinderFactory.Function() {
        @Override
        public Object invoke(Object obj, Object[] args) {
            return obj.toString();
        }
    };

    private static final OkBinderFactory.Function hashCode = new OkBinderFactory.Function() {
        @Override
        public Object invoke(Object obj, Object[] args) {
            return obj.hashCode();
        }
    };

    private static final OkBinderFactory.Function equals = new OkBinderFactory.Function() {
        @Override
        public Object invoke(Object obj, Object[] args) {
            return obj.equals(args[0]);
        }
    };

    public static void inject(OkBinderFactory.BaseBinder binder) {
        binder.register(TO_STRING, toString);
        binder.register(HASH_CODE, hashCode);
        binder.register(EQUALS, equals);
    }

    public static String toString(OkBinderFactory.BaseProxy proxy) {
        return (String) proxy.transact(0, TO_STRING);
    }

    public static int hashCode(OkBinderFactory.BaseProxy proxy) {
        return (int) proxy.transact(0, HASH_CODE);
    }

    public static boolean equals(OkBinderFactory.BaseProxy proxy, Object obj) {
        return (boolean) proxy.transact(0, EQUALS, obj);
    }
}
