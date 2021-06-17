package cn.thens.okbinder2.sample;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;

import cn.thens.okbinder2.OkBinder;

/**
 * @author 7hens
 */
public class MainActivity extends Activity implements ServiceConnection {
    private static final String TAG = "@OkBinder";
    boolean isServiceConnected = false;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Toast.makeText(this, "please check the log", Toast.LENGTH_SHORT).show();

        IRemoteService remoteService = OkBinder.proxy(IRemoteService.class, service);
        IRemoteService serviceA = new IRemoteServiceImpl("A");
        IRemoteService serviceB = new IRemoteServiceImpl("B");
        SparseArray<IRemoteService> sparseArray = new SparseArray<>();
        sparseArray.put(1, serviceA);
        sparseArray.put(2, serviceB);
        int[] intArray = {1, 2};

        remoteService.testVoid();
        log("testInt", remoteService.testInt(1234));
        log("testString", remoteService.testString("hello"));
        log("testCallback", remoteService.testCallback(serviceA));
        log("testList", remoteService.testList(Arrays.asList(serviceA, serviceB)));
        log("testSparseArray", remoteService.testSparseArray(sparseArray));
        log("testMap", remoteService.testMap(Collections.singletonMap("a", serviceA)));
        log("testAidlArray", remoteService.testAidlArray(new IRemoteService[]{serviceA, serviceB}));
        log("testObjectArray", remoteService.testObjectArray(new IRemoteService[]{serviceA, serviceB}));
        log("testPrimitiveArray", remoteService.testPrimitiveArray(intArray));
        log("testPrimitiveArray2", remoteService.testPrimitiveArray2(new int[][]{intArray, intArray}));
        try {
            remoteService.testError(false, ComponentName.unflattenFromString("a.b/.c"));
        } catch (Exception e) {
            log("testError", e);
        }
    }

    private void log(String tag, Object obj) {
        Log.d(TAG, "[CLIENT] " + tag + ": " + Utils.toString(obj));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.vTestLocalService).setOnClickListener(v -> rebindService(LocalService.class));
        findViewById(R.id.vTestRemoteService).setOnClickListener(v -> rebindService(RemoteService.class));
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
