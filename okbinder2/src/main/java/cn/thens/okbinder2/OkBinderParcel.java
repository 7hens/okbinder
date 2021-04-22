package cn.thens.okbinder2;

import android.os.IBinder;
import android.os.Parcel;
import android.util.SparseArray;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class OkBinderParcel {
    private static final int VAL_DEFAULT = 1;
    private static final int VAL_LIST = 2;
    private static final int VAL_SPARSE_ARRAY = 3;
    private static final int VAL_MAP = 4;
    private static final int VAL_OK_BINDER = 5;
    private static final int VAL_ARRAY = 6;

    public static void write(Parcel parcel, Object v) {
        if (v == null) {
            parcel.writeInt(VAL_DEFAULT);
            parcel.writeValue(null);
        } else if (v instanceof List) {
            parcel.writeInt(VAL_LIST);
            List val = (List) v;
            parcel.writeInt(val.size());
            for (Object o : val) {
                write(parcel, o);
            }
        } else if (v instanceof SparseArray) {
            parcel.writeInt(VAL_SPARSE_ARRAY);
            SparseArray val = (SparseArray) v;
            int size = val.size();
            parcel.writeInt(size);
            for (int i = 0; i < size; i++) {
                parcel.writeInt(val.keyAt(i));
                write(parcel, val.valueAt(i));
            }
        } else if (v instanceof Map) {
            parcel.writeInt(VAL_MAP);
            Map val = (Map) v;
            Set<Map.Entry<Object, Object>> entries = val.entrySet();
            parcel.writeInt(entries.size());
            for (Map.Entry<Object, Object> e : entries) {
                write(parcel, e.getKey());
                write(parcel, e.getValue());
            }
        } else if (v.getClass().isArray()) {
            parcel.writeInt(VAL_ARRAY);
            Class<?> componentType = v.getClass().getComponentType();
            //noinspection ConstantConditions
            if (componentType.isPrimitive()) {
                parcel.writeString("");
                parcel.writeValue(v);
                return;
            }
            parcel.writeString(componentType.getName());
            Object[] val = (Object[]) v;
            parcel.writeInt(val.length);
            for (Object o : val) {
                write(parcel, o);
            }
        } else {
            Class<?> okBinderInterface = OkBinder.getOkBinderInterface(v);
            if (okBinderInterface != null) {
                parcel.writeInt(VAL_OK_BINDER);
                parcel.writeString(okBinderInterface.getName());
                parcel.writeValue(OkBinder.create((Class<Object>) okBinderInterface, v));
                return;
            }
            parcel.writeInt(VAL_DEFAULT);
            parcel.writeValue(v);
        }
    }

    public static Object read(Parcel parcel, ClassLoader loader) throws ClassNotFoundException {
        int type = parcel.readInt();
        switch (type) {
            case VAL_LIST: {
                List outVal = new ArrayList();
                int size = parcel.readInt();
                for (int i = 0; i < size; i++) {
                    outVal.add(read(parcel, loader));
                }
                return outVal;
            }
            case VAL_SPARSE_ARRAY: {
                SparseArray outVal = new SparseArray<>();
                int size = parcel.readInt();
                for (int i = 0; i < size; i++) {
                    int key = parcel.readInt();
                    Object value = read(parcel, loader);
                    outVal.put(key, value);
                }
                return outVal;
            }
            case VAL_MAP: {
                Map outVal = new HashMap();
                int size = parcel.readInt();
                for (int i = 0; i < size; i++) {
                    Object key = read(parcel, loader);
                    Object value = read(parcel, loader);
                    outVal.put(key, value);
                }
                return outVal;
            }
            case VAL_OK_BINDER: {
                Class<?> serviceClass = loader.loadClass(parcel.readString());
                IBinder binder = (IBinder) parcel.readValue(loader);
                return OkBinder.proxy(serviceClass, binder);
            }
            case VAL_ARRAY: {
                String componentName = parcel.readString();
                boolean isPrimitiveComponent = "".equals(componentName);
                if (isPrimitiveComponent) {
                    return parcel.readValue(loader);
                }
                Class<?> componentClass = loader.loadClass(componentName);
                int size = parcel.readInt();
                Object[] outputVal = (Object[]) Array.newInstance(componentClass, size);
                for (int i = 0; i < size; i++) {
                    outputVal[i] = read(parcel, loader);
                }
                return outputVal;
            }
            default:
                return parcel.readValue(loader);
        }
    }
}
