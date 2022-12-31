package cn.thens.okbinder2;

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
        ProcessingHelper h = new ProcessingHelper(processingEnv);
        resolveAidl(h, env);
        resolveGenParcelable(h, env);
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Stream.of(AIDL.class, GenParcelable.class)
                .map(Class::getCanonicalName)
                .collect(Collectors.toSet());
    }

    private void resolveAidl(ProcessingHelper h, RoundEnvironment env) {
        for (Element element : env.getElementsAnnotatedWith(AIDL.class)) {
            checkIsInterface(element);
            new OkBinderFactoryGenerator(h, (TypeElement) element).generate();
        }
    }

    private void resolveGenParcelable(ProcessingHelper h, RoundEnvironment env) {
        for (Element element : env.getElementsAnnotatedWith(GenParcelable.class)) {
            checkIsInterface(element);
            TypeElement typeElement = (TypeElement) element;
            List<ExecutableElement> methods = h.getAllMethods(typeElement).stream()
                    .filter(m -> ElementUtils.isOverridable(m)
                            && !ElementUtils.isMemberOf(m, Object.class)
                            && !ElementUtils.isMemberOf(m, "android.os.Parcelable"))
                    .collect(Collectors.toList());
//            new DataBaseGenerator(h, typeElement, methods).generate();
//            new DataWrapperGenerator(h, typeElement, methods).generate();
//            new DataImplGenerator(h, typeElement, methods).generate();
            new DataParcelableGenerator(h, typeElement, methods).generate();
        }
    }

    private static void checkIsInterface(Element element) {
        Validate.isTrue(element.getKind().isInterface(), "Not an interface: %s", element);
    }

}