package cn.thens.okbinder2;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.ExecutableElement;

public class DataImplGenerator {
    private final ProcessingHelper h;
    private final List<ExecutableElement> methods;
    private final ClassName resultClass;

    public DataImplGenerator(ProcessingHelper h, List<ExecutableElement> methods, ClassName resultClass) {
        this.h = h;
        this.methods = methods;
        this.resultClass = resultClass;
    }

    public void generate() {
        h.writeJavaFile(TypeSpec.classBuilder(resultClass)
                .addModifiers(PUBLIC)
                .superclass(h.newClassName("Base"))
                .addMethod(noParamsConstructor())
                .addMethod(fullParamsConstructor())
                .addMethod(createFromDataMethod())
                .addFields(dataFields())
                .addMethods(dataMethods())
                .addMethods(setterMethods())
                .build());
    }

    public List<FieldSpec> dataFields() {
        return methods.stream()
                .map(m -> JavaPoetUtils.toFieldBuilder(m)
                        .addModifiers(PRIVATE)
                        .build())
                .collect(Collectors.toList());
    }

    public MethodSpec noParamsConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .build();
    }

    public MethodSpec fullParamsConstructor() {
        List<ParameterSpec> params = methods.stream()
                .map(m -> JavaPoetUtils.toParameterBuilder(m).build())
                .collect(Collectors.toList());

        CodeBlock statements = JavaPoetUtils.statements(methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("this.$L($L)", name, name))
                .collect(Collectors.toList()));

        return MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameters(params)
                .addCode(statements)
                .build();
    }

    public MethodSpec createFromDataMethod() {
        TypeName targetClass = h.getElementType();
        CodeBlock statements = JavaPoetUtils.statements(methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("data.$L(source.$L())", name, name))
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("from")
                .addModifiers(PUBLIC, STATIC)
                .addParameter(ParameterSpec.builder(targetClass, "source").build())
                .addStatement("$T data = new $T()", resultClass, resultClass)
                .returns(resultClass)
                .addCode(statements)
                .addStatement("return data")
                .build();
    }

    public List<MethodSpec> dataMethods() {
        return methods.stream()
                .map(m -> JavaPoetUtils.toOverrideBuilder(m)
                        .addStatement("return $L", m.getSimpleName())
                        .build())
                .collect(Collectors.toList());
    }

    public List<MethodSpec> setterMethods() {
        return methods.stream()
                .map(m -> {
                    String name = m.getSimpleName().toString();
                    TypeName type = ClassName.get(m.getReturnType());
                    return MethodSpec.methodBuilder(name)
                            .addModifiers(PUBLIC)
                            .addParameter(ParameterSpec.builder(type, "value").build())
                            .returns(resultClass)
                            .addStatement("this.$L = value", name)
                            .addStatement("return this")
                            .build();
                })
                .collect(Collectors.toList());
    }
}
