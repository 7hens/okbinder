package cn.thens.okbinder2.sample_library;

import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;

import java.util.List;
import java.util.Map;

/**
 * @author 7hens
 */
public final class IRemoteServiceImpl implements IRemoteService {
    private final String name;

    public IRemoteServiceImpl(String name) {
        this.name = name;
    }

    private void log(String tag, Object obj) {
        LogUtils.log("--------------------------------------");
        LogUtils.log("[" + name + "] " + tag + ": " + Utils.toString(obj));
    }

    @Override
    public void testVoid() {
        log("testVoid", null);
    }

    @Override
    public int testInt(int i) {
        log("testInt", i);
        return i;
    }

    @Override
    public String testString(String text) {
        log("testString", text);
        return text;
    }

    @Override
    public GeneralData testParcelable(GeneralData generalData) {
        log("testParcelable", generalData);
        return generalData;
    }

    @Override
    public void testError(Boolean aBoolean, Parcelable aParcelable) {
        log("testError", new Object[]{aBoolean, aParcelable});
        throw new NullPointerException();
    }

    @Override
    public IRemoteService testCallback(IRemoteService callback) {
        log("testCallback", callback);
        callback.testVoid();
        return callback;
    }

    @Override
    public List<IRemoteService> testList(List<IRemoteService> list) {
        log("testList", list);
        return list;
    }

    @Override
    public SparseArray<IRemoteService> testSparseArray(SparseArray<IRemoteService> sparseArray) {
        log("testSparseArray", sparseArray);
        return sparseArray;
    }

    @Override
    public Map<String, IRemoteService> testMap(Map<String, IRemoteService> map) {
        log("testMap", map);
        return map;
    }

    @Override
    public IRemoteService[] testAidlArray(IRemoteService[] array) {
        log("testAidlArray", array);
        return array;
    }

    @Override
    public Object[] testObjectArray(Object[] array) {
        log("testObjectArray", array);
        return array;
    }

    @Override
    public int[] testPrimitiveArray(int[] array) {
        log("testPrimitiveArray", array);
        return array;
    }

    @Override
    public int[][] testPrimitiveArray2(int[][] array) {
        log("testPrimitiveArray2", array);
        return array;
    }

    @Override
    public String toString() {
        return "IParcelableImpl_" + name;
    }
}
