package cn.thens.okbinder2;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

public class JavaPoetUtils {

    public static CodeBlock join(CodeBlock delimiter, Iterable<CodeBlock> elements) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (CodeBlock element : elements) {
            if (!builder.isEmpty()) {
                builder.add(delimiter);
            }
            builder.add(element);
        }
        return builder.build();
    }

    public static CodeBlock join(String delimiter, Iterable<CodeBlock> elements) {
        return join(CodeBlock.of(delimiter), elements);
    }

    public static CodeBlock statements(Iterable<CodeBlock> elements) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (CodeBlock element : elements) {
            builder.addStatement(element);
        }
        return builder.build();
    }

    public static AnnotationSpec toAnnotation(AnnotationMirror annotation) {
        return AnnotationSpec.get(annotation);
    }

    public static ParameterSpec toParameter(VariableElement param) {
        return ParameterSpec.get(param);
    }

    public static TypeName toType(TypeMirror type) {
        return TypeName.get(type);
    }

    public static FieldSpec.Builder toFieldBuilder(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        return FieldSpec.builder(toType(method.getReturnType()), methodName);
    }

    public static MethodSpec.Builder toOverrideBuilder(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(ClassName.get(Override.class))
                .addModifiers(method.getModifiers().stream()
                        .filter(modifier -> modifier != Modifier.ABSTRACT)
                        .collect(Collectors.toSet()))
                .addParameters(method.getParameters().stream()
                        .map(JavaPoetUtils::toParameter)
                        .collect(Collectors.toList()))
                .returns(toType(method.getReturnType()));
    }

    public static ParameterSpec.Builder toParameterBuilder(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        return ParameterSpec.builder(toType(method.getReturnType()), methodName);
    }

    public static MethodSpec.Builder toMethodBuilder(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        return MethodSpec.methodBuilder(methodName)
                .addAnnotations(method.getAnnotationMirrors().stream()
                        .map(JavaPoetUtils::toAnnotation)
                        .collect(Collectors.toList()))
                .addModifiers(method.getModifiers())
                .addParameters(method.getParameters().stream()
                        .map(JavaPoetUtils::toParameter)
                        .collect(Collectors.toList()))
                .returns(toType(method.getReturnType()));
    }
}
