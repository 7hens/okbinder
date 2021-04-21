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

    IRemoteService testList(List<IRemoteService> list);

    IRemoteService testSparseArray(SparseArray<IRemoteService> sparseArray);

    IRemoteService testMap(Map<String, IRemoteService> map);

    IRemoteService testArray(IRemoteService[] array);

    IRemoteService testObjectArray(Object[] array);
}
