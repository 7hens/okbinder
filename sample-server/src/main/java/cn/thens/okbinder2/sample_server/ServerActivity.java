package cn.thens.okbinder2.sample_server;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import cn.thens.okbinder2.sample_library.LogUtils;

/**
 * @author 7hens
 */
public class ServerActivity extends Activity implements LogUtils.Printer {
    private TextView logView;
    private String logs = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
}
