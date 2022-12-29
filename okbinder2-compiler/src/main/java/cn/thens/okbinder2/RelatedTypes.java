package cn.thens.okbinder2;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeVariableName;

final class RelatedTypes {
    private static final String ANDROID_OS = "android.os";
    ClassName Binder = ClassName.get(ANDROID_OS, "Binder");
    ClassName IBinder = ClassName.get(ANDROID_OS, "IBinder");
    ClassName Parcelable = ClassName.get(ANDROID_OS, "Parcelable");
    ClassName ParcelableCreator = Parcelable.nestedClass("Creator");
    ClassName Parcel = ClassName.get(ANDROID_OS, "Parcel");
    ClassName Override = ClassName.get(Override.class);
    ClassName Throwable = ClassName.get(Throwable.class);

    private static final String OK_BINDER = "cn.thens.okbinder2";
    ClassName OkBinderFactory = ClassName.get(OK_BINDER, "OkBinderFactory");
    ClassName Function = OkBinderFactory.nestedClass("Function");
    ClassName BaseBinder = OkBinderFactory.nestedClass("BaseBinder");
    ClassName BaseProxy = OkBinderFactory.nestedClass("BaseProxy");

    ClassName String = ClassName.get(String.class);
    ClassName Class = ClassName.get(Class.class);
}
