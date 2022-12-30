package cn.thens.okbinder2;

public final class OkBinderVersion {
    public static final int C0 = 0;
    public static final int C20 = 20;
    public static final int C201 = 201;

    public static Spec current() {
        return Spec.V2_1;
    }

    public static Spec of(int version) {
        for (Spec value : Spec.values()) {
            if (version == value.code) {
                return value;
            }
        }
        return Spec.V0;
    }

    public enum Spec {
        V0(C0, C0, C0),
        V2_0(C20, C20, C20),
        V2_1(C201, C20, C201);

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
}

