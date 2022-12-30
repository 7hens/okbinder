package cn.thens.okbinder2;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

final class ProcessingHelper {
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

    private final ProcessingEnvironment env;

    ProcessingHelper(ProcessingEnvironment env) {
        this.env = env;
    }

    public ProcessingEnvironment env() {
        return env;
    }

    public TypeElement asElement(TypeMirror type) {
        return ((TypeElement) env.getTypeUtils().asElement(type));
    }

    public List<ExecutableElement> getAllMethods(TypeElement element) {
        return env.getElementUtils().getAllMembers(element).stream()
                .filter(member -> member instanceof ExecutableElement)
                .map(member -> (ExecutableElement) member)
                .collect(Collectors.toList());
    }

    public String getPackageName(Element element) {
        return env.getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }

    public void writeJavaFile(String packageName, TypeSpec typeSpec) {
        try {
            JavaFile.builder(packageName, typeSpec)
                    .indent("    ")
                    .build()
                    .writeTo(env.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
