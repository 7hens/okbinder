package cn.thens.okbinder2;

import android.os.Parcelable;

public final class OkParcelable {

    public static <P extends Parcelable> Parcelable.Creator<P> creator() {
        throw new RuntimeException();
    }

    public static Parcelable parcelable(Object value) {
        throw new RuntimeException();
    }
}
