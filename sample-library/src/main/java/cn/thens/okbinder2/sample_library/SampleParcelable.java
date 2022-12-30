package cn.thens.okbinder2.sample_library;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.ArrayMap;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import java.io.FileDescriptor;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

public class SampleParcelable implements Parcelable {
    private final boolean pBoolean;
    private final boolean[] pBooleanArray;
//    private final boolean[][] pBooleanArray2;

    private final Boolean bBoolean;
//    private final Boolean[] bBooleanArray;
//    private final List<Boolean> booleanList;

    private final int pInteger;
    private final int[] pIntegerArray;

    private final Integer bInteger;
//    private final Integer[] bIntegerArray;
//    private final List<Integer> bIntegerList;

    private final String string;
    private final String[] stringArray;
    private final List<String> stringList;

    private final SampleParcelable parcelable;
    private final SampleParcelable[] parcelableArray;

    protected SampleParcelable(Parcel in) {
        pBoolean = in.readByte() != 0;
        pBooleanArray = in.createBooleanArray();
        byte tmpBBoolean = in.readByte();
        bBoolean = tmpBBoolean == 0 ? null : tmpBBoolean == 1;
        pInteger = in.readInt();
        pIntegerArray = in.createIntArray();
        if (in.readByte() == 0) {
            bInteger = null;
        } else {
            bInteger = in.readInt();
        }
        string = in.readString();
        stringArray = in.createStringArray();
        stringList = in.createStringArrayList();
        parcelable = in.readParcelable(SampleParcelable.class.getClassLoader());
        parcelableArray = in.createTypedArray(SampleParcelable.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (pBoolean ? 1 : 0));
        dest.writeBooleanArray(pBooleanArray);
        dest.writeByte((byte) (bBoolean == null ? 0 : bBoolean ? 1 : 2));
        dest.writeInt(pInteger);
        dest.writeIntArray(pIntegerArray);
        if (bInteger == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(bInteger);
        }
        dest.writeString(string);
        dest.writeStringArray(stringArray);
        dest.writeStringList(stringList);
        dest.writeParcelable(parcelable, flags);
        dest.writeTypedArray(parcelableArray, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SampleParcelable> CREATOR = new Creator<SampleParcelable>() {
        @Override
        public SampleParcelable createFromParcel(Parcel in) {
            return new SampleParcelable(in);
        }

        @Override
        public SampleParcelable[] newArray(int size) {
            return new SampleParcelable[size];
        }
    };

    /**
     * @see Parcel#createBinderArray()
     * @see Parcel#createBinderArrayList()
     * @see Parcel#createBooleanArray()
     * @see Parcel#createByteArray()
     * @see Parcel#createCharArray()
     * @see Parcel#createDoubleArray()
     * @see Parcel#createFixedArray(Class, int...)
     * @see Parcel#createFixedArray(Class, Parcelable.Creator, int...)
     * @see Parcel#createFixedArray(Class, Function, int...)
     * @see Parcel#createFloatArray()
     * @see Parcel#createIntArray()
     * @see Parcel#createInterfaceArray(IntFunction, Function)
     * @see Parcel#createInterfaceArrayList(Function)
     * @see Parcel#createLongArray()
     * @see Parcel#createStringArray()
     * @see Parcel#createStringArrayList()
     * @see Parcel#createTypedArray(Parcelable.Creator)
     * @see Parcel#createTypedArrayList(Parcelable.Creator)
     * @see Parcel#createTypedArrayMap(Parcelable.Creator)
     * @see Parcel#createTypedSparseArray(Parcelable.Creator)
     * @see Parcel#readArray(ClassLoader, Class)
     * @see Parcel#readArray(ClassLoader)
     * @see Parcel#readArrayList(ClassLoader, Class)
     * @see Parcel#readArrayList(ClassLoader)
     * @see Parcel#readBinderArray(IBinder[])
     * @see Parcel#readBinderList(List)
     * @see Parcel#readBlob()
     * @see Parcel#readBoolean()
     * @see Parcel#readBooleanArray(boolean[])
     * @see Parcel#readBundle()
     * @see Parcel#readBundle(ClassLoader)
     * @see Parcel#readByte()
     * @see Parcel#readByteArray(byte[])
     * @see Parcel#readCharArray(char[])
     * @see Parcel#readDouble()
     * @see Parcel#readDoubleArray(double[])
     * @see Parcel#readException()
     * @see Parcel#readException(int, String)
     * @see Parcel#readFileDescriptor()
     * @see Parcel#readFixedArray(Object)
     * @see Parcel#readFixedArray(Object, Parcelable.Creator)
     * @see Parcel#readFixedArray(Object, Function)
     * @see Parcel#readFloat()
     * @see Parcel#readFloatArray(float[])
     * @see Parcel#readHashMap(ClassLoader, Class, Class)
     * @see Parcel#readHashMap(ClassLoader)
     * @see Parcel#readInt()
     * @see Parcel#readIntArray(int[])
     * @see Parcel#readInterfaceArray(IInterface[], Function)
     * @see Parcel#readInterfaceList(List, Function)
     * @see Parcel#readList(List, ClassLoader, Class)
     * @see Parcel#readList(List, ClassLoader)
     * @see Parcel#readLong()
     * @see Parcel#readLongArray(long[])
     * @see Parcel#readMap(Map, ClassLoader, Class, Class)
     * @see Parcel#readMap(Map, ClassLoader)
     * @see Parcel#readParcelable(ClassLoader, Class)
     * @see Parcel#readParcelable(ClassLoader)
     * @see Parcel#readParcelableArray(ClassLoader, Class)
     * @see Parcel#readParcelableArray(ClassLoader)
     * @see Parcel#readParcelableCreator(ClassLoader, Class)
     * @see Parcel#readParcelableList(List, ClassLoader, Class)
     * @see Parcel#readPersistableBundle()
     * @see Parcel#readPersistableBundle(ClassLoader)
     * @see Parcel#readSerializable(ClassLoader, Class)
     * @see Parcel#readSize()
     * @see Parcel#readSizeF()
     * @see Parcel#readSparseArray(ClassLoader, Class)
     * @see Parcel#readSparseArray(ClassLoader)
     * @see Parcel#readSparseBooleanArray()
     * @see Parcel#readString()
     * @see Parcel#readStringArray(String[])
     * @see Parcel#readStringList(List)
     * @see Parcel#readStrongBinder()
     * @see Parcel#readTypedArray(Object[], Parcelable.Creator)
     * @see Parcel#readTypedList(List, Parcelable.Creator)
     * @see Parcel#readTypedObject(Parcelable.Creator)
     * @see Parcel#readValue(ClassLoader)
     */
    private void read() {
    }

    /**
     * @see Parcel#writeArray(Object[])
     * @see Parcel#writeBinderArray(IBinder[])
     * @see Parcel#writeBinderList(List)
     * @see Parcel#writeBlob(byte[])
     * @see Parcel#writeBlob(byte[], int, int)
     * @see Parcel#writeBoolean(boolean)
     * @see Parcel#writeBooleanArray(boolean[])
     * @see Parcel#writeBundle(Bundle)
     * @see Parcel#writeByte(byte)
     * @see Parcel#writeByteArray(byte[])
     * @see Parcel#writeByteArray(byte[], int, int)
     * @see Parcel#writeCharArray(char[])
     * @see Parcel#writeDouble(double)
     * @see Parcel#writeDoubleArray(double[])
     * @see Parcel#writeException(Exception)
     * @see Parcel#writeFileDescriptor(FileDescriptor)
     * @see Parcel#writeFloat(float)
     * @see Parcel#writeFloatArray(float[])
     * @see Parcel#writeFixedArray(Object, int, int...)
     * @see Parcel#writeInt(int)
     * @see Parcel#writeIntArray(int[])
     * @see Parcel#writeInterfaceArray(IInterface[])
     * @see Parcel#writeInterfaceList(List)
     * @see Parcel#writeInterfaceToken(String)
     * @see Parcel#writeList(List)
     * @see Parcel#writeLong(long)
     * @see Parcel#writeLongArray(long[])
     * @see Parcel#writeMap(Map)
     * @see Parcel#writeNoException()
     * @see Parcel#writeParcelable(Parcelable, int)
     * @see Parcel#writeParcelableArray(Parcelable[], int)
     * @see Parcel#writeParcelableCreator(Parcelable)
     * @see Parcel#writeParcelableList(List, int)
     * @see Parcel#writePersistableBundle(PersistableBundle)
     * @see Parcel#writeSerializable(Serializable)
     * @see Parcel#writeSize(Size)
     * @see Parcel#writeSizeF(SizeF)
     * @see Parcel#writeSparseArray(SparseArray)
     * @see Parcel#writeSparseBooleanArray(SparseBooleanArray)
     * @see Parcel#writeString(String)
     * @see Parcel#writeStringArray(String[])
     * @see Parcel#writeStringList(List)
     * @see Parcel#writeStrongBinder(IBinder)
     * @see Parcel#writeStrongInterface(IInterface)
     * @see Parcel#writeTypedArray(Parcelable[], int)
     * @see Parcel#writeTypedArrayMap(ArrayMap, int)
     * @see Parcel#writeTypedList(List)
     * @see Parcel#writeTypedObject(Parcelable, int)
     * @see Parcel#writeTypedSparseArray(SparseArray, int)
     * @see Parcel#writeValue(Object)
     */
    private void write() {
    }

}
