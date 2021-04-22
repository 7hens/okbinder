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
    String test();

    void testError(Boolean aBoolean, Parcelable aParcelable);

    IRemoteService testCallback(IRemoteService callback);

    void testList(List<IRemoteService> list);

    void testSparseArray(SparseArray<IRemoteService> sparseArray);

    void testMap(Map<String, IRemoteService> map);

    void testAidlArray(IRemoteService[] array);

    void testObjectArray(Object[] array);

    void testPrimitiveArray(int[] array);

    void testPrimitiveArray2(int[][] array);
}
