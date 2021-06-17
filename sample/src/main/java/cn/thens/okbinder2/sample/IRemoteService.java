package cn.thens.okbinder2.sample;

import android.os.Parcelable;
import android.util.SparseArray;

import java.util.List;
import java.util.Map;

import cn.thens.okbinder2.AIDL;

/**
 * @author 7hens
 */
@SuppressWarnings("UnusedReturnValue")
@AIDL
interface IRemoteService {
    void testVoid();

    int testInt(int i);

    String testString(String text);

    void testError(Boolean aBoolean, Parcelable aParcelable);

    IRemoteService testCallback(IRemoteService callback);

    List<IRemoteService> testList(List<IRemoteService> list);

    SparseArray<IRemoteService> testSparseArray(SparseArray<IRemoteService> sparseArray);

    Map<String, IRemoteService> testMap(Map<String, IRemoteService> map);

    IRemoteService[]  testAidlArray(IRemoteService[] array);

    Object[] testObjectArray(Object[] array);

    int[] testPrimitiveArray(int[] array);

    int[][] testPrimitiveArray2(int[][] array);
}
