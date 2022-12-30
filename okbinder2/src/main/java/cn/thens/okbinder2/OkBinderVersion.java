package cn.thens.okbinder2;

public final class OkBinderVersion {
    public static final int V20 = 20;
    public static final int V201 = 201;

    public enum Spec {
        V2_0(V20, V20, V20),
        V2_1(V201, V20, V201);

        public final int code;

        public final int minSupported;

        public final int maxSupported;

        Spec(int code, int minSupported, int maxSupported) {
            this.code = code;
            this.minSupported = minSupported;
            this.maxSupported = maxSupported;
        }

        public boolean isSupported(int version) {
            return version >= minSupported && version <= maxSupported;
        }
    }

    public static Spec current() {
        return Spec.V2_1;
    }
}

