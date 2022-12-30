package cn.thens.okbinder2;

import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public final class OkBinderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        ProcessingHelper t = new ProcessingHelper(processingEnv);

        for (Element element : env.getElementsAnnotatedWith(AIDL.class)) {
            new OkBinderFactoryGenerator(t, (TypeElement) element).generate();
        }
        for (Element element : env.getElementsAnnotatedWith(GenParcelable.class)) {
            new ParcelableGenerator(t, (TypeElement) element).generate();
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(AIDL.class.getCanonicalName());
    }
}