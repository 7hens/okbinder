package cn.thens.okbinder2;

import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public final class OkBinderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        Elements elementUtils = processingEnv.getElementUtils();
        Filer filer = processingEnv.getFiler();
        RelatedTypes t = new RelatedTypes();

        for (Element element : env.getElementsAnnotatedWith(AIDL.class)) {
            TypeElement typeElement = (TypeElement) element;
        }
        for (Element element : env.getElementsAnnotatedWith(GenParcelable.class)) {
            new OkBinderParcelableGenerator(t, processingEnv, (TypeElement) element).generate();
        }

        generate(new OkBinderFactoryGenerator(t, elementUtils, filer),
                env.getElementsAnnotatedWith(AIDL.class));
        return false;
    }

    private void generate(TypeElementGenerator generator, Set<? extends Element> elements) {
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element;
            generator.generate(typeElement);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(AIDL.class.getCanonicalName());
    }
}