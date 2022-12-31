package cn.thens.okbinder2;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
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

public class DataWrapperGenerator {
    private final ProcessingHelper h;
    private final TypeElement element;
    private final List<ExecutableElement> methods;

    public DataWrapperGenerator(ProcessingHelper h, TypeElement element, List<ExecutableElement> methods) {
        this.h = h;
        this.element = element;
        this.methods = methods;
    }

    public void generate() {
        TypeName targetClass = ClassName.get(element.asType());
        ClassName cWrapper = h.newClassName(element, "Wrapper");

        h.writeJavaFile(element, TypeSpec.classBuilder(cWrapper)
                .addModifiers(PUBLIC, ABSTRACT)
                .superclass(h.newClassName(element, "Base"))
                .addField(FieldSpec.builder(targetClass, "data", PRIVATE, FINAL).build())
                .addMethod(wrapConstructor(targetClass))
                .addMethods(dataMethods())
                .build());
    }

    public MethodSpec wrapConstructor(TypeName targetClass) {
        return MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(targetClass, "data").build())
                .addStatement(CodeBlock.of("this.data = data"))
                .build();
    }

    public List<MethodSpec> dataMethods() {
        return methods.stream()
                .map(m -> JavaPoetUtils.toOverrideBuilder(m)
                        .addStatement("return data.$L()", m.getSimpleName())
                        .build())
                .collect(Collectors.toList());
    }
}
