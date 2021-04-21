package cn.thens.okbinder2.sample;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import cn.thens.okbinder2.OkBinder;

/**
 * @author 7hens
 */
public class RemoteService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return OkBinder.create(new IRemoteServiceImpl("RemoteService"));
    }
}
