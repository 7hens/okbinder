package cn.thens.okbinder2.sample_library;

import android.util.Log;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class LogUtils {
    private static final String TAG = "@OkBinder";
    private static final Set<Printer> printers = new CopyOnWriteArraySet<>();

    public static void addPrinter(Printer printer) {
        printers.add(printer);
    }

    public static void removePrinter(Printer printer) {
        printers.remove(printer);
    }

    public static void log(Object message) {
        String msg = Utils.toString(message);
        Log.d(TAG, msg);
        String time = String.format(Locale.getDefault(), "%03d", System.currentTimeMillis() % 1000);
        for (Printer printer : printers) {
            printer.print(time + "| " + msg);
        }
    }

    public interface Printer {
        void print(String message);
    }
}
