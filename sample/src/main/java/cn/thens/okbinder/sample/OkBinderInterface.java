package cn.thens.okbinder.sample;

import android.os.Parcelable;
import android.util.SparseArray;

import java.util.List;
import java.util.Map;

import cn.thens.okbinder.OkBinder;

/**
 * @author 7hens
 */
@SuppressWarnings("UnusedReturnValue")
@OkBinder.Interface
interface OkBinderInterface {
    String test();

    void testError(Boolean aBoolean, Parcelable aParcelable);

    OkBinderInterface testCallback(OkBinderInterface callback);

    OkBinderInterface testList(List<OkBinderInterface> list);

    OkBinderInterface testSparseArray(SparseArray<OkBinderInterface> sparseArray);

    OkBinderInterface testMap(Map<String, OkBinderInterface> map);

    OkBinderInterface testArray(OkBinderInterface[] array);

    OkBinderInterface testObjectArray(Object[] array);
}
