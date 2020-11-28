package cn.thens.okbinder.sample;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import cn.thens.okbinder.OkBinder;

/**
 * @author 7hens
 */
public class RemoteService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return OkBinder.create(new OkBinderInterfaceImpl("RemoteService"));
    }
}
