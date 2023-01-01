package cn.thens.okbinder2;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
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
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

public class DataParcelableGenerator {
    private final ProcessingHelper h;
    private final List<ExecutableElement> methods;
    private final ClassName resultClass;

    public DataParcelableGenerator(ProcessingHelper h, List<ExecutableElement> methods, ClassName resultClass) {
        this.h = h;
        this.methods = methods;
        this.resultClass = resultClass;
    }

    public void generate() {
        DataBaseGenerator dataBaseGenerator = new DataBaseGenerator(h, methods, resultClass);
        DataImplGenerator dataImplGenerator = new DataImplGenerator(h, methods, resultClass);

        h.writeJavaFile(TypeSpec.classBuilder(resultClass)
                .addModifiers(PUBLIC)
                .addSuperinterface(h.getElementType())
                .addSuperinterface(h.cParcelable)
                .addField(creatorField())
                .addMethod(describeContentsMethod())
                .addMethod(writeToParcelMethod())
                .addMethod(createFromParcelMethod())
                .addMethod(dataImplGenerator.createFromDataMethod())
                .addFields(dataImplGenerator.dataFields())
                .addMethod(dataImplGenerator.noParamsConstructor())
                .addMethod(dataImplGenerator.fullParamsConstructor())
                .addMethod(dataImplGenerator.mergeWithMethod())
                .addMethods(dataImplGenerator.dataMethods())
                .addMethods(dataImplGenerator.setterMethods())
                .addMethod(dataBaseGenerator.equalsMethod())
                .addMethod(dataBaseGenerator.hashCodeMethod())
                .addMethod(dataBaseGenerator.toStringMethod())
                .addType(creatorClass())
                .build());
    }

    private MethodSpec wrapConstructor(TypeName cTarget) {
        return MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(cTarget, "data").build())
                .addStatement(CodeBlock.of("super(data)"))
                .build();
    }

    private MethodSpec createFromParcelMethod() {
        CodeBlock statements = JavaPoetUtils.statements(methods.stream()
                .map(this::readParcelableCode)
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("from")
                .addModifiers(PUBLIC, STATIC)
                .addParameter(ParameterSpec.builder(h.cParcel, "source").build())
                .returns(resultClass)
                .addStatement("$T data = new $T()", resultClass, resultClass)
                .addCode(statements)
                .addStatement("return data", resultClass)
                .build();
    }

    private CodeBlock readParcelableCode(ExecutableElement method) {
        String name = method.getSimpleName().toString();
        TypeMirror type = method.getReturnType();
        String readMethodName = ParcelCodeHelper.getReadMethodNameForType(type);
        if ("readParcelable".equals(readMethodName)) {
            return CodeBlock.of("data.$L(source.$L(data.getClass().getClassLoader()))", name, readMethodName);
        }
        if ("createTypedArray".equals(readMethodName)) {
            TypeMirror componentType = ((ArrayType) type).getComponentType();
            return CodeBlock.of("data.$L(source.$L($T.CREATOR))", name, readMethodName, TypeName.get(componentType));
        }
        return CodeBlock.of("data.$L(source.$L())", name, readMethodName);
    }

    private MethodSpec writeToParcelMethod() {
        CodeBlock statements = JavaPoetUtils.statements(methods.stream()
                .map(this::writeToParcelCode)
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("writeToParcel")
                .addAnnotation(h.cOverride)
                .addModifiers(PUBLIC)
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

    private MethodSpec describeContentsMethod() {
        return MethodSpec.methodBuilder("describeContents")
                .addAnnotation(h.cOverride)
                .addModifiers(PUBLIC)
                .returns(int.class)
                .addStatement("return 0")
                .build();
    }

    private FieldSpec creatorField() {
        ClassName cCreator = resultClass.nestedClass("Creator");
        return FieldSpec.builder(cCreator, "CREATOR", PUBLIC, STATIC, FINAL)
                .initializer(CodeBlock.of("new $T()", cCreator))
                .build();
    }

    private TypeSpec creatorClass() {
        ClassName cCreator = resultClass.nestedClass("Creator");
        TypeName cParcelableCreator = ParameterizedTypeName.get(h.cParcelableCreator, resultClass);
        return TypeSpec.classBuilder(cCreator)
                .addModifiers(PUBLIC, STATIC)
                .addSuperinterface(cParcelableCreator)
                .addMethod(MethodSpec.methodBuilder("createFromParcel")
                        .addAnnotation(h.cOverride)
                        .addModifiers(PUBLIC)
                        .addParameter(ParameterSpec.builder(h.cParcel, "source").build())
                        .returns(resultClass)
                        .addStatement("return from(source)")
                        .build())
                .addMethod(MethodSpec.methodBuilder("newArray")
                        .addAnnotation(h.cOverride)
                        .addModifiers(PUBLIC)
                        .addParameter(ParameterSpec.builder(int.class, "size").build())
                        .returns(ArrayTypeName.of(resultClass))
                        .addStatement("return new $T[$L]", resultClass, "size")
                        .build())
                .build();
    }
}
