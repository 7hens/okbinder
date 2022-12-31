package cn.thens.okbinder2;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class DataBaseGenerator {
    private final ProcessingHelper h;
    private final TypeElement element;
    private final List<ExecutableElement> methods;

    public DataBaseGenerator(ProcessingHelper h, TypeElement element, List<ExecutableElement> methods) {
        this.h = h;
        this.element = element;
        this.methods = methods;
    }

    public void generate() {
        ClassName cBase = h.newClassName(element, "Base");
        h.writeJavaFile(element, TypeSpec.classBuilder(cBase)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addSuperinterface(ClassName.get(element.asType()))
                .addMethod(equalsMethod())
                .addMethod(hashCodeMethod())
                .addMethod(toStringMethod(cBase))
                .addMethod(toDataStringMethod())
                .build());
    }

    public MethodSpec equalsMethod() {
        TypeName targetClass = ClassName.get(element.asType());

        CodeBlock joinedComparisons = JavaPoetUtils.join(" && \n", methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("$T.equals($L(), other.$L())", h.cObjects, name, name))
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("equals")
                .addAnnotation(h.cOverride)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(h.cObject, "obj").build())
                .returns(boolean.class)
                .addCode(CodeBlock.builder()
                        .beginControlFlow("if (obj == this)", "obj")
                        .addStatement("return true")
                        .endControlFlow()
                        .beginControlFlow("if (!(obj instanceof $T))", targetClass)
                        .addStatement("return false")
                        .endControlFlow()
                        .addStatement("$T other = ($T) obj", targetClass, targetClass)
                        .addStatement("return $L", joinedComparisons)
                        .build())
                .build();
    }

    public MethodSpec hashCodeMethod() {
        CodeBlock hashCodeArgs = JavaPoetUtils.join(", \n", methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("$L()", name))
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("hashCode")
                .addAnnotation(h.cOverride)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return $T.hashCode(new $T[]{$L})", h.cArrays, h.cObject, hashCodeArgs)
                .build();
    }

    public MethodSpec toStringMethod(TypeName myClass) {
        CodeBlock fieldCodes = JavaPoetUtils.join(", ", methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("$L=\" + $L() \n+ \"", name, name))
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("toString")
                .addAnnotation(h.cOverride)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return \"$T($L)\"", myClass, fieldCodes)
                .build();
    }

    public MethodSpec toDataStringMethod() {
        CodeBlock fieldCodes = JavaPoetUtils.join(", ", methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("$L=\" + $L() \n+ \"", name, name))
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("toDataString")
                .addModifiers(Modifier.PROTECTED)
                .returns(String.class)
                .addStatement("return \"($L)\"", fieldCodes)
                .build();
    }
}
