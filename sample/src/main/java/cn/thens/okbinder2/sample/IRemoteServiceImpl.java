package cn.thens.okbinder2.sample;

import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author 7hens
 */
public final class IRemoteServiceImpl implements IRemoteService {
    private static final String TAG = "@OkBinder";
    private final String tag;

    IRemoteServiceImpl(String tag) {
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
    public IRemoteService testCallback(IRemoteService callback) {
        Log.d(TAG, tag + ".testCallback(" + callback + ") ");
        callback.test();
        return callback;
    }

    @Override
    public IRemoteService testList(List<IRemoteService> list) {
        Log.d(TAG, tag + ".testList(" + list + ") ");
        return null;
    }

    @Override
    public IRemoteService testSparseArray(SparseArray<IRemoteService> sparseArray) {
        Log.d(TAG, tag + ".testSparseArray(" + sparseArray + ") ");
        return null;
    }

    @Override
    public IRemoteService testMap(Map<String, IRemoteService> map) {
        Log.d(TAG, tag + ".testMap(" + map + ") ");
        return null;
    }

    @Override
    public IRemoteService testArray(IRemoteService[] array) {
        Log.d(TAG, tag + ".testArray(" + Arrays.toString(array) + ") ");
        return null;
    }

    @Override
    public IRemoteService testObjectArray(Object[] array) {
        Log.d(TAG, tag + ".testObjectArray(" + Arrays.toString(array) + ") ");
        return null;
    }

    @Override
    public String toString() {
        return "IParcelableImpl{" + tag + "}";
    }
}
