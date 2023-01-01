package cn.thens.okbinder2;

import com.squareup.javapoet.ClassName;

import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public final class OkBinderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        resolveAidl(env);
        resolveGenParcelable(env);
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Stream.of(AIDL.class, GenParcelable.class)
                .map(Class::getCanonicalName)
                .collect(Collectors.toSet());
    }

    private void resolveAidl(RoundEnvironment env) {
        for (Element element : env.getElementsAnnotatedWith(AIDL.class)) {
            checkIsInterface(element);
            TypeElement typeElement = (TypeElement) element;
            ProcessingHelper h = new ProcessingHelper(processingEnv, typeElement);
            List<ExecutableElement> methods = h.getAllMethods().stream()
                    .filter(m -> ElementUtils.isOverridable(m)
                            && !ElementUtils.isMemberOf(m, Object.class))
                    .collect(Collectors.toList());
            new OkBinderFactoryGenerator(h, methods, h.newClassName("Factory")).generate();
        }
    }

    private void resolveGenParcelable(RoundEnvironment env) {
        for (Element element : env.getElementsAnnotatedWith(GenParcelable.class)) {
            checkIsInterface(element);
            TypeElement typeElement = (TypeElement) element;
            ProcessingHelper h = new ProcessingHelper(processingEnv, typeElement);
            ClassName resultClass = h.newClassName("Parcelable");
            List<ExecutableElement> methods = h.getAllMethods().stream()
                    .filter(m -> ElementUtils.isOverridable(m)
                            && !ElementUtils.isMemberOf(m, Object.class)
                            && !ElementUtils.isMemberOf(m, "android.os.Parcelable"))
                    .collect(Collectors.toList());
//            new DataBaseGenerator(h, typeElement, methods).generate();
//            new DataWrapperGenerator(h, typeElement, methods).generate();
//            new DataImplGenerator(h, typeElement, methods).generate();
            new DataParcelableGenerator(h, methods, resultClass).generate();
        }
    }

    private static void checkIsInterface(Element element) {
        Validate.isTrue(element.getKind().isInterface(), "Not an interface: %s", element);
    }

}