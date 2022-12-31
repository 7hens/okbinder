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
        ClassName cCreator = cParcelable.nestedClass("Creator");
        DataBaseGenerator dataBaseGenerator = new DataBaseGenerator(h, element, methods);
        DataImplGenerator dataImplGenerator = new DataImplGenerator(h, element, methods);

        h.writeJavaFile(element, TypeSpec.classBuilder(cParcelable)
                .addModifiers(PUBLIC)
                .addSuperinterface(cTarget)
                .addSuperinterface(h.cParcelable)
                .addField(creatorField(cCreator))
                .addMethod(describeContentsMethod())
                .addMethod(writeToParcelMethod())
                .addMethod(createFromParcelMethod(cParcelable))
                .addMethod(dataImplGenerator.createFromDataMethod(cParcelable))
                .addFields(dataImplGenerator.dataFields())
                .addMethod(dataImplGenerator.noParamsConstructor())
                .addMethod(dataImplGenerator.fullParamsConstructor())
                .addMethods(dataImplGenerator.dataMethods())
                .addMethods(dataImplGenerator.setterMethods(cParcelable))
                .addMethod(dataBaseGenerator.equalsMethod())
                .addMethod(dataBaseGenerator.hashCodeMethod())
                .addMethod(dataBaseGenerator.toStringMethod(cParcelable))
                .addType(creatorClass(cCreator, cParcelable))
                .build());
    }

    private MethodSpec wrapConstructor(TypeName cTarget) {
        return MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(cTarget, "data").build())
                .addStatement(CodeBlock.of("super(data)"))
                .build();
    }

    private MethodSpec createFromParcelMethod(ClassName myClass) {
        CodeBlock statements = JavaPoetUtils.statements(methods.stream()
                .map(this::readParcelableCode)
                .collect(Collectors.toList()));

        return MethodSpec.methodBuilder("from")
                .addModifiers(PUBLIC, STATIC)
                .addParameter(ParameterSpec.builder(h.cParcel, "source").build())
                .returns(myClass)
                .addStatement("$T data = new $T()", myClass, myClass)
                .addCode(statements)
                .addStatement("return data", myClass)
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

    private FieldSpec creatorField(ClassName cCreator) {
        return FieldSpec.builder(cCreator, "CREATOR", PUBLIC, STATIC, FINAL)
                .initializer(CodeBlock.of("new $T()", cCreator))
                .build();
    }

    private TypeSpec creatorClass(ClassName cCreator, TypeName cParcelable) {
        TypeName cParcelableCreator = ParameterizedTypeName.get(h.cParcelableCreator, cParcelable);
        return TypeSpec.classBuilder(cCreator)
                .addModifiers(PUBLIC, STATIC)
                .addSuperinterface(cParcelableCreator)
                .addMethod(MethodSpec.methodBuilder("createFromParcel")
                        .addAnnotation(h.cOverride)
                        .addModifiers(PUBLIC)
                        .addParameter(ParameterSpec.builder(h.cParcel, "source").build())
                        .returns(cParcelable)
                        .addStatement("return from(source)")
                        .build())
                .addMethod(MethodSpec.methodBuilder("newArray")
                        .addAnnotation(h.cOverride)
                        .addModifiers(PUBLIC)
                        .addParameter(ParameterSpec.builder(int.class, "size").build())
                        .returns(ArrayTypeName.of(cParcelable))
                        .addStatement("return new $T[$L]", cParcelable, "size")
                        .build())
                .build();
    }
}
