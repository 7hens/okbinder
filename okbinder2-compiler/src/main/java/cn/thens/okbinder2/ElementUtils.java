package cn.thens.okbinder2;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

final class ElementUtils {

    public static boolean isOverridable(ExecutableElement method) {
        return !containsAny(method.getModifiers(), Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isObjectMethod(ExecutableElement method) {
        return isType(method.getEnclosingElement().asType(), Object.class);
    }

    public static boolean isMemberOf(Element element, Class<?> cls) {
        return isType(getDeclaringType(element), cls);
    }

    public static boolean isMemberOf(Element element, String cls) {
        return getDeclaringType(element).toString().equals(cls);
    }

    public static TypeMirror getDeclaringType(Element element) {
        return element.getEnclosingElement().asType();
    }

    public static boolean isType(TypeMirror typeMirror, Class<?> cls) {
        return typeMirror.toString().equals(cls.getName());
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean containsAny(Iterable<T> iterable, T... elements) {
        Set<T> set = new HashSet<>();
        Collections.addAll(set, elements);
        for (T element : iterable) {
            if (set.contains(element)) {
                return true;
            }
        }
        return false;
    }
}
