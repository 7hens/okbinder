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
    public void testList(List<IRemoteService> list) {
        Log.d(TAG, tag + ".testList(" + list + ") ");
    }

    @Override
    public void testSparseArray(SparseArray<IRemoteService> sparseArray) {
        Log.d(TAG, tag + ".testSparseArray(" + sparseArray + ") ");
    }

    @Override
    public void testMap(Map<String, IRemoteService> map) {
        Log.d(TAG, tag + ".testMap(" + map + ") ");
    }

    @Override
    public void testAidlArray(IRemoteService[] array) {
        Log.d(TAG, tag + ".testAidlArray(" + Arrays.toString(array) + ") ");
    }

    @Override
    public void testObjectArray(Object[] array) {
        Log.d(TAG, tag + ".testObjectArray(" + Arrays.toString(array) + ") ");
    }

    @Override
    public void testPrimitiveArray(int[] array) {
        Log.d(TAG, tag + ".testPrimitiveArray(" + Arrays.toString(array) + ") ");
    }

    @Override
    public void testPrimitiveArray2(int[][] array) {
        Log.d(TAG, tag + ".testPrimitiveArray2(" + Arrays.toString(array[0]) + ") ");
    }

    @Override
    public String toString() {
        return "IParcelableImpl{" + tag + "}";
    }
}
