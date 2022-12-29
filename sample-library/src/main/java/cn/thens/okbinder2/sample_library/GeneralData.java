package cn.thens.okbinder2.sample_library;

import cn.thens.okbinder2.GenParcelable;

@GenParcelable
public interface GeneralData {

    String name();

    Boolean isEnabled();

    SampleParcelable parcelable();

    int[] intArray();

    float[] floatArray();

    SampleParcelable[] parcelableArray();
}
