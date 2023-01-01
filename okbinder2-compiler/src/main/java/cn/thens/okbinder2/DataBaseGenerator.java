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

public class DataBaseGenerator {
    private final ProcessingHelper h;
    private final List<ExecutableElement> methods;
    private final ClassName resultClass;

    public DataBaseGenerator(ProcessingHelper h, List<ExecutableElement> methods, ClassName resultClass) {
        this.h = h;
        this.methods = methods;
        this.resultClass = resultClass;
    }

    public void generate() {
//        ClassName cBase = h.newClassName(element, "Base");
        h.writeJavaFile(TypeSpec.classBuilder(resultClass)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addSuperinterface(h.getElementType())
                .addMethod(equalsMethod())
                .addMethod(hashCodeMethod())
                .addMethod(toStringMethod())
                .addMethod(toDataStringMethod())
                .build());
    }

    public MethodSpec equalsMethod() {
        TypeName elementType = h.getElementType();

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
                        .beginControlFlow("if (!(obj instanceof $T))", elementType)
                        .addStatement("return false")
                        .endControlFlow()
                        .addStatement("$T other = ($T) obj", elementType, elementType)
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

    public MethodSpec toStringMethod() {
        CodeBlock fieldCodes = JavaPoetUtils.join(", ", methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("$L=\" + $L() \n+ \"", name, name))
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("toString")
                .addAnnotation(h.cOverride)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return \"$T($L)\"", resultClass, fieldCodes)
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
