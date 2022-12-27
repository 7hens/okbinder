package cn.thens.okbinder2;

import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public final class OkBinderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        Elements elementUtils = processingEnv.getElementUtils();
        Set<? extends Element> elements = env.getElementsAnnotatedWith(AIDL.class);
        RelatedTypes t = new RelatedTypes();
        OkBinderFactoryGenerator generator = new OkBinderFactoryGenerator(t, elementUtils, processingEnv.getFiler());
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element;
            generator.generate(typeElement);
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(AIDL.class.getCanonicalName());
    }
}