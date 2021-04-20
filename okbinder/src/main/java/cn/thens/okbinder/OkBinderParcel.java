package cn.thens.okbinder;

import android.os.IBinder;
import android.os.Parcel;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class OkBinderParcel {
    private static final int VAL_DEFAULT = 1;
    private static final int VAL_LIST = 2;
    private static final int VAL_SPARSEARRAY = 3;
    private static final int VAL_MAP = 4;
    private static final int VAL_OKBINDER = 5;

    public static void writeValue(Parcel parcel, Object v) {
        if (v == null) {
            parcel.writeInt(VAL_DEFAULT);
            parcel.writeValue(null);
        } else if (v instanceof List) {
            parcel.writeInt(VAL_LIST);
            List val = (List) v;
            int size = val.size();
            parcel.writeInt(size);
            for (int i = 0; i < size; i++) {
                writeValue(parcel, val.get(i));
            }
        } else if (v instanceof SparseArray) {
            parcel.writeInt(VAL_SPARSEARRAY);
            SparseArray val = (SparseArray) v;
            int size = val.size();
            parcel.writeInt(size);
            for (int i = 0; i < size; i++) {
                parcel.writeInt(val.keyAt(i));
                writeValue(parcel, val.valueAt(i));
            }
        } else if (v instanceof Map) {
            parcel.writeInt(VAL_MAP);
            Map val = (Map) v;
            Set<Map.Entry<Object, Object>> entries = val.entrySet();
            parcel.writeInt(entries.size());
            for (Map.Entry<Object, Object> e : entries) {
                writeValue(parcel, e.getKey());
                writeValue(parcel, e.getValue());
            }
        } else {
            Class<?> okBinderInterface = OkBinder.getOkBinderInterface(v);
            if (okBinderInterface != null) {
                parcel.writeInt(VAL_OKBINDER);
                parcel.writeString(okBinderInterface.getName());
                parcel.writeValue(OkBinder.create(v, (Class<Object>) okBinderInterface));
                return;
            }
            parcel.writeInt(VAL_DEFAULT);
            parcel.writeValue(v);
        }
    }

    public static Object readValue(Parcel parcel, ClassLoader loader) throws ClassNotFoundException {
        int type = parcel.readInt();
        switch (type) {
            case VAL_LIST: {
                List outVal = new ArrayList();
                for (int size = parcel.readInt(); size >= 0; size--) {
                    outVal.add(readValue(parcel, loader));
                }
                return outVal;
            }
            case VAL_SPARSEARRAY: {
                SparseArray outVal = new SparseArray<>();
                for (int size = parcel.readInt(); size >= 0; size--) {
                    int key = parcel.readInt();
                    Object value = readValue(parcel, loader);
                    outVal.put(key, value);
                }
                return outVal;
            }
            case VAL_MAP: {
                Map outVal = new HashMap();
                for (int size = parcel.readInt(); size >= 0; size--) {
                    Object key = readValue(parcel, loader);
                    Object value = readValue(parcel, loader);
                    outVal.put(key, value);
                }
                return outVal;
            }
            case VAL_OKBINDER: {
                Class<?> serviceClass = loader.loadClass(parcel.readString());
                IBinder binder = (IBinder) parcel.readValue(loader);
                return OkBinder.proxy(binder, serviceClass);
            }
            default:
                return parcel.readValue(loader);
        }
    }

}
