package cn.thens.okbinder2.sample_library;

import android.os.Parcelable;

import cn.thens.okbinder2.GenParcelable;

@GenParcelable
public interface GeneralData extends Parcelable {

    String name();

    Boolean isEnabled();

    SampleParcelable parcelable();

    int pInteger();

    int[] intArray();

    float[] floatArray();

    SampleParcelable[] parcelableArray();
}
