package cn.thens.okbinder.sample;

import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author 7hens
 */
public final class OkBinderInterfaceImpl implements OkBinderInterface {
    private static final String TAG = "@OkBinder";
    private final String tag;

    OkBinderInterfaceImpl(String tag) {
        this.tag = tag;
    }

    @Override
    public String test() {
        Log.d(TAG, tag + ".test()");
        return tag + ".test";
    }

    @Override
    public void testError(Boolean aBoolean, Parcelable aParcelable) {
        Log.d(TAG, tag + ".testError(" + aBoolean + ", " + aParcelable + ")");
        throw new NullPointerException();
    }

    @Override
    public OkBinderInterface testCallback(OkBinderInterface callback) {
        Log.d(TAG, tag + ".testCallback(" + callback + ") ");
        callback.test();
        return callback;
    }

    @Override
    public OkBinderInterface testList(List<OkBinderInterface> list) {
        Log.d(TAG, tag + ".testList(" + list + ") ");
        return null;
    }

    @Override
    public OkBinderInterface testSparseArray(SparseArray<OkBinderInterface> sparseArray) {
        Log.d(TAG, tag + ".testSparseArray(" + sparseArray + ") ");
        return null;
    }

    @Override
    public OkBinderInterface testMap(Map<String, OkBinderInterface> map) {
        Log.d(TAG, tag + ".testMap(" + map + ") ");
        return null;
    }

    @Override
    public OkBinderInterface testArray(OkBinderInterface[] array) {
        Log.d(TAG, tag + ".testArray(" + Arrays.toString(array) + ") ");
        return null;
    }

    @Override
    public OkBinderInterface testObjectArray(Object[] array) {
        Log.d(TAG, tag + ".testObjectArray(" + Arrays.toString(array) + ") ");
        return null;
    }

    @Override
    public String toString() {
        return "RemoteInterfaceImpl{" + tag + "}";
    }
}
