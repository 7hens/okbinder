package cn.thens.okbinder2;

public final class OkBinderVersion {
    public static final int MAGIC_NUMBER = -1588420922;
    public static final int V20 = 20;

    public static int current() {
        return V20;
    }

    public static int minSupported() {
        return V20;
    }

    public static boolean isSupported(int version) {
        return version >= minSupported();
    }
}
