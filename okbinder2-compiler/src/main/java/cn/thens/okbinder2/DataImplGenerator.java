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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class DataImplGenerator {
    private final ProcessingHelper h;
    private final TypeElement element;
    private final List<ExecutableElement> methods;

    public DataImplGenerator(ProcessingHelper h, TypeElement element, List<ExecutableElement> methods) {
        this.h = h;
        this.element = element;
        this.methods = methods;
    }

    public void generate() {
        ClassName cImpl = h.newClassName(element, "Impl");

        h.writeJavaFile(element, TypeSpec.classBuilder(cImpl)
                .addModifiers(PUBLIC)
                .superclass(h.newClassName(element, "Base"))
                .addMethod(noParamsConstructor())
                .addMethod(fullParamsConstructor())
                .addMethod(createFromDataMethod(cImpl))
                .addFields(dataFields())
                .addMethods(dataMethods())
                .addMethods(setterMethods(cImpl))
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

    public MethodSpec createFromDataMethod(TypeName myClass) {
        TypeName targetClass = ClassName.get(element.asType());
        CodeBlock statements = JavaPoetUtils.statements(methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("data.$L(source.$L())", name, name))
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("from")
                .addModifiers(PUBLIC, STATIC)
                .addParameter(ParameterSpec.builder(targetClass, "source").build())
                .addStatement("$T data = new $T()", myClass, myClass)
                .returns(myClass)
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

    public List<MethodSpec> setterMethods(TypeName myClass) {
        return methods.stream()
                .map(m -> {
                    String name = m.getSimpleName().toString();
                    TypeName type = ClassName.get(m.getReturnType());
                    return MethodSpec.methodBuilder(name)
                            .addModifiers(PUBLIC)
                            .addParameter(ParameterSpec.builder(type, "value").build())
                            .returns(myClass)
                            .addStatement("this.$L = value", name)
                            .addStatement("return this")
                            .build();
                })
                .collect(Collectors.toList());
    }
}
