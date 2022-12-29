package cn.thens.okbinder2.sample_library;

import android.os.Parcel;
import android.os.Parcelable;

public class SampleParcelable implements Parcelable {
    private final String name;
    private final Boolean isEnabled;
    private final boolean primitiveBoolean;
    private final boolean[] privateBooleanArray;
    private final SampleParcelable parcelable;
    private final SampleParcelable[] parcelableArray;

    protected SampleParcelable(Parcel in) {
        name = in.readString();
        byte tmpIsEnabled = in.readByte();
        isEnabled = tmpIsEnabled == 0 ? null : tmpIsEnabled == 1;
        primitiveBoolean = in.readByte() != 0;
        privateBooleanArray = in.createBooleanArray();
        parcelable = in.readParcelable(SampleParcelable.class.getClassLoader());
        parcelableArray = in.createTypedArray(SampleParcelable.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeByte((byte) (isEnabled == null ? 0 : isEnabled ? 1 : 2));
        dest.writeByte((byte) (primitiveBoolean ? 1 : 0));
        dest.writeBooleanArray(privateBooleanArray);
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
}
