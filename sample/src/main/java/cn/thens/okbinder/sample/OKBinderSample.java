package cn.thens.okbinder.sample;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import cn.thens.okbinder.OkBinder;

public class OKBinderSample {
    private static final String TAG = "@OkBinder";

    interface IRemoteService {
        String test();

        void testError(Boolean aBoolean, Parcelable aParcelable);

        OkBinder<IRemoteService> testCallback(OkBinder<IRemoteService> callback);
    }

    public static class MainActivity extends Activity implements ServiceConnection {
        boolean isServiceConnected = false;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(this, "please check the log", Toast.LENGTH_SHORT).show();
            IRemoteService remoteService = OkBinder.client(IRemoteService.class, service);
            Log.d(TAG, "client: test => " + remoteService.test());
            try {
                remoteService.testError(false, ComponentName.unflattenFromString("a.b/.c"));
            } catch (Exception e) {
                Log.e(TAG, "client: testError => \n" + Log.getStackTraceString(e));
            }
            try {
                IRemoteService callback = OkBinder.client(remoteService.testCallback(createRemoteInterface("callback")));
            } catch (Throwable  e) {
                Log.e(TAG, "client: callback error => \n" + Log.getStackTraceString(e));
            }
            Log.d(TAG, "client: callback");
            Log.d(TAG, "end of onServiceConnected");
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            findViewById(R.id.vTestLocalService)
                    .setOnClickListener(v -> rebindService(LocalService.class));
            findViewById(R.id.vTestRemoteService)
                    .setOnClickListener(v -> rebindService(RemoteService.class));
        }

        private void rebindService(Class<?> serviceClass) {
            if (isServiceConnected) {
                unbindService(this);
            }
            bindService(new Intent(this, serviceClass), this, Context.BIND_AUTO_CREATE);
            isServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    public static abstract class BaseService extends Service {
        private Binder okBinder = createRemoteInterface("service");

        @Override
        public IBinder onBind(Intent intent) {
            return okBinder;
        }
    }

    public static class LocalService extends BaseService {
    }

    public static class RemoteService extends BaseService {
    }

    private static OkBinder<IRemoteService> createRemoteInterface(String tag) {
        return OkBinder.server(new IRemoteService() {
            @Override
            public String test() {
                Log.d(TAG, ">> ** " + tag + ": test ** <<");
                return tag + ".test";
            }

            @Override
            public void testError(Boolean aBoolean, Parcelable aParcelable) {
                Log.d(TAG, ">> ** " + tag + ": testError <<");
                throw new NullPointerException();
            }

            @Override
            public OkBinder<IRemoteService> testCallback(OkBinder<IRemoteService> callback) {
                Log.d(TAG, ">> ** " + tag + ": testCallback ** <<");
                IRemoteService remoteInterface = OkBinder.client(callback);
                remoteInterface.test();
                return callback;
            }
        });
    }

}
