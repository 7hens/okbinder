package cn.thens.okbinder;

final class ObjectFunctions {
    private static final String TO_STRING = "toString:1B2M2Y8AsgTpgAmY7PhCfg==";
    private static final String HASH_CODE = "hashCode:1B2M2Y8AsgTpgAmY7PhCfg==";
    private static final String EQUALS = "equals:CNhnhoCENfIbauVfLxTHwA==";

    private static final OkBinderFactory.Function FUNC_TO_STRING = new OkBinderFactory.Function() {
        @Override
        public Object invoke(Object obj, Object[] args) {
            return obj.toString();
        }
    };

    private static final OkBinderFactory.Function FUNC_HASH_CODE = new OkBinderFactory.Function() {
        @Override
        public Object invoke(Object obj, Object[] args) {
            return obj.hashCode();
        }
    };

    private static final OkBinderFactory.Function FUNC_EQUALS = new OkBinderFactory.Function() {
        @Override
        public Object invoke(Object obj, Object[] args) {
            return obj.equals(args[0]);
        }
    };

    public static void inject(OkBinderFactory.BaseBinder binder) {
        binder.addFunction(TO_STRING, FUNC_TO_STRING);
        binder.addFunction(HASH_CODE, FUNC_HASH_CODE);
        binder.addFunction(EQUALS, FUNC_EQUALS);
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
