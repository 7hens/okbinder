package cn.thens.okbinder2;

import com.squareup.javapoet.ClassName;

final class RelatedTypes {
    private static final String BINDER = "android.os";
    ClassName Binder = ClassName.get(BINDER, "Binder");
    ClassName IBinder = ClassName.get(BINDER, "IBinder");
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
