package cn.thens.okbinder2;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

final class ProcessingHelper {
    private static final String ANDROID_OS = "android.os";
    ClassName cBinder = ClassName.get(ANDROID_OS, "Binder");
    ClassName cIBinder = ClassName.get(ANDROID_OS, "IBinder");
    ClassName cParcelable = ClassName.get(ANDROID_OS, "Parcelable");
    ClassName cParcelableCreator = cParcelable.nestedClass("Creator");
    ClassName cParcel = ClassName.get(ANDROID_OS, "Parcel");
    ClassName cOverride = ClassName.get(Override.class);
    ClassName cThrowable = ClassName.get(Throwable.class);

    private static final String OK_BINDER = "cn.thens.okbinder2";
    ClassName cOkBinderFactory = ClassName.get(OK_BINDER, "OkBinderFactory");
    ClassName cFunction = cOkBinderFactory.nestedClass("Function");
    ClassName cBaseBinder = cOkBinderFactory.nestedClass("BaseBinder");
    ClassName cBaseProxy = cOkBinderFactory.nestedClass("BaseProxy");

    ClassName cObject = ClassName.OBJECT;
    ClassName cString = ClassName.get(String.class);
    ClassName cObjects = ClassName.get(Objects.class);
    ClassName cArrays = ClassName.get(Arrays.class);
    ClassName cClass = ClassName.get(Class.class);

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

    public TypeMirror asTypeMirror(TypeName type) {
        return env.getElementUtils().getTypeElement(type.toString()).asType();
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

    public ClassName newClassName(Element element, String suffix) {
        return ClassName.get(getPackageName(element), element.getSimpleName() + suffix);
    }

    public void writeJavaFile(Element element, TypeSpec typeSpec) {
        writeJavaFile(getPackageName(element), typeSpec);
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
