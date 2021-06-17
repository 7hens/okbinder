package cn.thens.okbinder2.sample_client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import cn.thens.okbinder2.sample_library.IRemoteServiceImpl;
import cn.thens.okbinder2.OkBinder;

/**
 * @author 7hens
 */
public class LocalService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return OkBinder.create(new IRemoteServiceImpl("LocalService"));
    }
}
