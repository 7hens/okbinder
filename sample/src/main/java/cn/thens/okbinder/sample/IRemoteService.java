package cn.thens.okbinder.sample;

import android.os.Parcelable;

import cn.thens.okbinder.AIDL;
import cn.thens.okbinder.InOut;
import cn.thens.okbinder.OkBinder;
import cn.thens.okbinder.OneWay;

@OkBinder.Interface
@AIDL
interface IRemoteService {
    String test();

    @OneWay
    void testError(Boolean aBoolean, @InOut Parcelable aParcelable);

    IRemoteService testCallback(IRemoteService callback);
}
