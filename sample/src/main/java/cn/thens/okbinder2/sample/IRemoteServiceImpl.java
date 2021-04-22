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

    private String log(String text) {
        String msg = toString() + "." + text;
        Log.d(TAG, msg);
        return msg;
    }
    
    @Override
    public String test() {
        return log("test()");
    }

    @Override
    public void testError(Boolean aBoolean, Parcelable aParcelable) {
        log("testError(" + aBoolean + ", " + aParcelable + ")");
        throw new NullPointerException();
    }

    @Override
    public IRemoteService testCallback(IRemoteService callback) {
        log("testCallback(" + callback + ") ");
        callback.test();
        return callback;
    }

    @Override
    public void testList(List<IRemoteService> list) {
        log("testList(" + list + ") ");
    }

    @Override
    public void testSparseArray(SparseArray<IRemoteService> sparseArray) {
        log("testSparseArray(" + sparseArray + ") ");
    }

    @Override
    public void testMap(Map<String, IRemoteService> map) {
        log("testMap(" + map + ") ");
    }

    @Override
    public void testAidlArray(IRemoteService[] array) {
        log("testAidlArray(" + Arrays.toString(array) + ") ");
    }

    @Override
    public void testObjectArray(Object[] array) {
        log("testObjectArray(" + Arrays.toString(array) + ") ");
    }

    @Override
    public void testPrimitiveArray(int[] array) {
        log("testPrimitiveArray(" + Arrays.toString(array) + ") ");
    }

    @Override
    public void testPrimitiveArray2(int[][] array) {
        log("testPrimitiveArray2(" + Arrays.toString(array[0]) + ") ");
    }

    @Override
    public String toString() {
        return "IParcelableImpl_" + tag;
    }
}
