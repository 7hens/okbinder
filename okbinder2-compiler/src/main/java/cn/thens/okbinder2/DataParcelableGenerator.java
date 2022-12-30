package cn.thens.okbinder2;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

public class DataParcelableGenerator {
    private final ProcessingHelper h;
    private final TypeElement element;
    private final List<ExecutableElement> methods;

    public DataParcelableGenerator(ProcessingHelper h, TypeElement element, List<ExecutableElement> methods) {
        this.h = h;
        this.element = element;
        this.methods = methods;
    }

    public void generate() {
        TypeName cTarget = ClassName.get(element.asType());
        ClassName cParcelable = h.newClassName(element, "Parcelable");
        TypeName cCreator = ParameterizedTypeName.get(h.cParcelableCreator, cParcelable);

        h.writeJavaFile(element, TypeSpec.classBuilder(cParcelable)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(cTarget)
                .addSuperinterface(h.cParcelable)
                .addField(FieldSpec.builder(cCreator, "CREATOR", Modifier.PUBLIC, STATIC, FINAL)
                        .initializer(CodeBlock.of("$L", anonymousCreatorClass(cCreator, cParcelable)))
                        .build())
                .addMethod(fullParamsConstructor())
                .addMethod(wrapConstructor(cTarget))
                .addMethod(parcelableConstructor())
                .addMethod(writeToParcelMethod())
                .addMethod(MethodSpec.methodBuilder("describeContents")
                        .addAnnotation(h.cOverride)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(int.class)
                        .addStatement("return 0")
                        .build())
                .addFields(dataFields())
                .addMethods(dataMethods())
                .addMethod(equalsMethod())
                .addMethod(hashCodeMethod())
                .addMethod(toStringMethod(cParcelable))
                .build());
    }

    private List<FieldSpec> dataFields() {
        return methods.stream()
                .map(m -> JavaPoetUtils.toFieldBuilder(m)
                        .addModifiers(PRIVATE, FINAL)
                        .build())
                .collect(Collectors.toList());
    }

    private List<MethodSpec> dataMethods() {
        return methods.stream()
                .map(m -> JavaPoetUtils.toOverrideBuilder(m)
                        .addStatement("return $L", m.getSimpleName())
                        .build())
                .collect(Collectors.toList());
    }

    private MethodSpec fullParamsConstructor() {
        List<ParameterSpec> params = methods.stream()
                .map(m -> JavaPoetUtils.toParameterBuilder(m).build())
                .collect(Collectors.toList());

        CodeBlock statements = JavaPoetUtils.statements(methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("this.$L = $L", name, name))
                .collect(Collectors.toList()));

        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameters(params)
                .addCode(statements)
                .build();
    }

    private MethodSpec wrapConstructor(TypeName cTarget) {
        CodeBlock statements = JavaPoetUtils.statements(methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("this.$L = data.$L()", name, name))
                .collect(Collectors.toList()));

        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(cTarget, "data").build())
                .addCode(statements)
                .build();
    }

    private MethodSpec parcelableConstructor() {
        CodeBlock statements = JavaPoetUtils.statements(methods.stream()
                .map(this::readParcelableCode)
                .collect(Collectors.toList()));

        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(h.cParcel, "source").build())
                .addCode(statements)
                .build();
    }

    private CodeBlock readParcelableCode(ExecutableElement method) {
        String name = method.getSimpleName().toString();
        TypeMirror type = method.getReturnType();
        String readMethodName = ParcelCodeHelper.getReadMethodNameForType(type);
        if ("readParcelable".equals(readMethodName)) {
            return CodeBlock.of("this.$L = source.$L(getClass().getClassLoader())", name, readMethodName);
        }
        if ("createTypedArray".equals(readMethodName)) {
            TypeMirror componentType = ((ArrayType) type).getComponentType();
            return CodeBlock.of("this.$L = source.$L($T.CREATOR)", name, readMethodName, TypeName.get(componentType));
        }
        return CodeBlock.of("this.$L = source.$L()", name, readMethodName);
    }

    private MethodSpec writeToParcelMethod() {
        CodeBlock statements = JavaPoetUtils.statements(methods.stream()
                .map(this::writeToParcelCode)
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("writeToParcel")
                .addAnnotation(h.cOverride)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(h.cParcel, "dest").build())
                .addParameter(ParameterSpec.builder(int.class, "flags").build())
                .returns(void.class)
                .addCode(statements)
                .build();
    }

    private CodeBlock writeToParcelCode(ExecutableElement method) {
        String name = method.getSimpleName().toString();
        TypeMirror type = method.getReturnType();
        String writeMethodName = ParcelCodeHelper.getWriteMethodNameForType(type);
        if ("writeParcelable".equals(writeMethodName) || "writeTypedArray".equals(writeMethodName)) {
            return CodeBlock.of("dest.$L($L(), flags)", writeMethodName, name);
        }
        return CodeBlock.of("dest.$L($L())", writeMethodName, name);
    }

    private TypeSpec anonymousCreatorClass(TypeName cCreator, TypeName cParcelable) {
        return TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(cCreator)
                .addMethod(MethodSpec.methodBuilder("createFromParcel")
                        .addAnnotation(h.cOverride)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(h.cParcel, "source").build())
                        .returns(cParcelable)
                        .addStatement("return new $T(source)", cParcelable)
                        .build())
                .addMethod(MethodSpec.methodBuilder("newArray")
                        .addAnnotation(h.cOverride)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(int.class, "size").build())
                        .returns(ArrayTypeName.of(cParcelable))
                        .addStatement("return new $T[$L]", cParcelable, "size")
                        .build())
                .build();
    }

    private MethodSpec equalsMethod() {
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

    private MethodSpec hashCodeMethod() {
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

    private MethodSpec toStringMethod(TypeName cParcelable) {
        CodeBlock fieldCodes = JavaPoetUtils.join(", ", methods.stream()
                .map(ExecutableElement::getSimpleName)
                .map(name -> CodeBlock.of("$L=\" + $L() \n+ \"", name, name))
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("toString")
                .addAnnotation(h.cOverride)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return \"$T($L)\"", cParcelable, fieldCodes)
                .build();
    }
}
