package cn.thens.okbinder2;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

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

public class DataWrapperGenerator {
    private final ProcessingHelper h;
    private final List<ExecutableElement> methods;
    private final ClassName resultClass;

    public DataWrapperGenerator(ProcessingHelper h, List<ExecutableElement> methods, ClassName resultClass) {
        this.h = h;
        this.methods = methods;
        this.resultClass = resultClass;
    }

    public void generate() {
        TypeName targetClass = h.getElementType();

        h.writeJavaFile(TypeSpec.classBuilder(resultClass)
                .addModifiers(PUBLIC, ABSTRACT)
                .superclass(h.newClassName("Base"))
                .addField(FieldSpec.builder(targetClass, "data", PRIVATE, FINAL).build())
                .addMethod(wrapConstructor())
                .addMethods(dataMethods())
                .build());
    }

    public MethodSpec wrapConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(resultClass, "data").build())
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
