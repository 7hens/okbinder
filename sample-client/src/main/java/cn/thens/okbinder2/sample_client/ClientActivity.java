package cn.thens.okbinder2.sample_client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.SparseArray;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;

import cn.thens.okbinder2.sample_library.IRemoteService;
import cn.thens.okbinder2.sample_library.IRemoteServiceImpl;
import cn.thens.okbinder2.sample_library.LogUtils;
import cn.thens.okbinder2.sample_library.Utils;
import cn.thens.okbinder2.OkBinder;

/**
 * @author 7hens
 */
public class ClientActivity extends Activity implements ServiceConnection, LogUtils.Printer {
    boolean isServiceConnected = false;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Toast.makeText(this, "please check the log", Toast.LENGTH_SHORT).show();

        IRemoteService remoteService = OkBinder.proxy(IRemoteService.class, service);
        IRemoteService serviceA = new IRemoteServiceImpl("ClientA");
        IRemoteService serviceB = new IRemoteServiceImpl("ClientB");
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

    private TextView logView;
    private String logs = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.vTestLocalService).setOnClickListener(v ->
                rebindService("cn.thens.okbinder2.sample_client/.LocalService"));

        findViewById(R.id.vTestRemoteService).setOnClickListener(v ->
                rebindService("cn.thens.okbinder2.sample_server/.RemoteService"));

        logView = findViewById(R.id.vLog);
        logView.setMovementMethod(ScrollingMovementMethod.getInstance());
        LogUtils.addPrinter(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.removePrinter(this);
    }

    @Override
    public void print(String message) {
        logs += "\n" + message;
        logView.setText(logs);
    }

    private void log(String tag, Object obj) {
        LogUtils.log(tag + ": " + Utils.toString(obj));
    }

    private void rebindService(String componentName) {
        logs = "";
        logView.setText("");
        if (isServiceConnected) {
            unbindService(this);
        }

        ComponentName component = ComponentName.unflattenFromString(componentName);
        bindService(new Intent().setComponent(component), this, Context.BIND_AUTO_CREATE);
        isServiceConnected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
}
