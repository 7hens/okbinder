package cn.thens.okbinder2;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

public class OkBinderParcelableGenerator {
    private final ProcessingHelper h;
    private final TypeElement element;

    private final String packageName;
    private final ClassName parcelableClassName;

    private final List<FieldSpec> fields = new ArrayList<>();
    private final List<MethodSpec> methods = new ArrayList<>();
    private final CodeBlock.Builder createFromParamCode = CodeBlock.builder();
    private final CodeBlock.Builder createFromDataCode = CodeBlock.builder();
    private final CodeBlock.Builder createFromParcelCode = CodeBlock.builder();
    private final CodeBlock.Builder writeToParcelCode = CodeBlock.builder();

    public OkBinderParcelableGenerator(ProcessingHelper h, TypeElement element) {
        this.h = h;
        this.element = element;

        packageName = h.getPackageName(element);
        parcelableClassName = ClassName.get(packageName, element.getSimpleName() + "Parcelable");

        Validate.isTrue(element.getKind().isInterface(), "Not an interface: %s", element.getKind());
    }

    public void generate() {
        h.getAllMethods(element).stream()
                .filter(method -> !ElementUtils.isObjectMethod(method) && ElementUtils.isOverridable(method))
                .forEach(this::buildCodes);
        generateParcelableClass();
    }

    private void generateParcelableClass() {
        try {
            JavaFile.builder(packageName, buildType())
                    .indent("    ")
                    .build()
                    .writeTo(h.env().getFiler());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void buildCodes(ExecutableElement method) {
        String name = method.getSimpleName().toString();
        TypeMirror type = method.getReturnType();
        TypeName typeName = TypeName.get(type);

        fields.add(FieldSpec.builder(typeName, name, Modifier.PRIVATE, Modifier.FINAL).build());
        methods.add(MethodSpec.methodBuilder(name)
                .addAnnotation(h.Override)
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addStatement("return $L", name)
                .build());

        createFromParamCode.addStatement("this.$L = $L", name, name);
        createFromDataCode.addStatement("this.$L = data.$L()", name, name);

        String readMethodName = ParcelCodeHelper.getReadMethodNameForType(type);
        if ("readParcelable".equals(readMethodName)) {
            createFromParcelCode.addStatement("this.$L = source.$L(getClass().getClassLoader())", name, readMethodName);
        } else if ("createTypedArray".equals(readMethodName)) {
            TypeMirror componentType = ((ArrayType) type).getComponentType();
            createFromParcelCode.addStatement("this.$L = source.$L($T.CREATOR)", name, readMethodName, TypeName.get(componentType));
        } else {
            createFromParcelCode.addStatement("this.$L = source.$L()", name, readMethodName);
        }

        String writeMethodName = ParcelCodeHelper.getWriteMethodNameForType(type);
        if ("writeParcelable".equals(writeMethodName) || "writeTypedArray".equals(writeMethodName)) {
            writeToParcelCode.addStatement("dest.$L($L(), flags)", writeMethodName, name);
        } else {
            writeToParcelCode.addStatement("dest.$L($L())", writeMethodName, name);
        }
    }

    private TypeSpec buildType() {
        TypeName targetClass = ClassName.get(element.asType());
        TypeName creatorClass = ParameterizedTypeName.get(h.ParcelableCreator, parcelableClassName);

        TypeSpec creatorImplClass = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(creatorClass)
                .addMethod(MethodSpec.methodBuilder("createFromParcel")
                        .addAnnotation(h.Override)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(h.Parcel, "source").build())
                        .returns(parcelableClassName)
                        .addStatement("return new $T(source)", parcelableClassName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("newArray")
                        .addAnnotation(h.Override)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(int.class, "size").build())
                        .returns(ArrayTypeName.of(parcelableClassName))
                        .addStatement("return new $T[$L]", parcelableClassName, "size")
                        .build())
                .build();

        return TypeSpec.classBuilder(element.getSimpleName().toString() + "Parcelable")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(targetClass)
                .addSuperinterface(h.Parcelable)
                .addFields(fields)
                .addField(FieldSpec.builder(creatorClass, "CREATOR", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(CodeBlock.of("$L", creatorImplClass))
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameters(fields.stream()
                                .map(field -> ParameterSpec.builder(field.type, field.name).build())
                                .collect(Collectors.toList()))
                        .addCode(createFromParamCode.build())
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(targetClass, "data").build())
                        .addCode(createFromDataCode.build())
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(h.Parcel, "source").build())
                        .addCode(createFromParcelCode.build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("writeToParcel")
                        .addAnnotation(h.Override)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(h.Parcel, "dest").build())
                        .addParameter(ParameterSpec.builder(int.class, "flags").build())
                        .returns(void.class)
                        .addCode(writeToParcelCode.build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("describeContents")
                        .addAnnotation(h.Override)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(int.class)
                        .addStatement("return 0")
                        .build())
                .addMethods(methods)
                .build();
    }
}
