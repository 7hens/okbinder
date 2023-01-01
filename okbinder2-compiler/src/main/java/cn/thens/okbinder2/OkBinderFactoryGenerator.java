package cn.thens.okbinder2;

import com.squareup.javapoet.ArrayTypeName;
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

public final class OkBinderFactoryGenerator {
    private final ProcessingHelper h;
    private final List<ExecutableElement> methods;
    private final ClassName resultClass;

    public OkBinderFactoryGenerator(ProcessingHelper h, List<ExecutableElement> methods, ClassName resultClass) {
        this.h = h;
        this.methods = methods;
        this.resultClass = resultClass;
    }

    public void generate() {
        h.writeJavaFile(TypeSpec.classBuilder(resultClass)
                .addModifiers(Modifier.FINAL)
                .addSuperinterface(h.cOkBinderFactory)
                .addMethod(newBuilderMethod())
                .addMethod(newProxyMethod())
                .addFields(factoryFields())
                .addType(myProxyType())
                .addType(myBuilderType())
                .build());
    }

    public List<FieldSpec> factoryFields() {
        return IndexedData.stream(methods)
                .map(m -> FieldSpec.builder(h.cString, m.data.getSimpleName() + "_" + m.index)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer(CodeBlock.of("$S", CompilerFunctionUtils.getFunctionId(m.data)))
                        .build())
                .collect(Collectors.toList());
    }

    public TypeSpec myBuilderType() {
        ClassName cMyBinder = resultClass.nestedClass("MyBinder");
        CodeBlock binderFunctionCode = JavaPoetUtils.statements(IndexedData.stream(methods)
                .map(m -> binderFunctionCode(m.data, m.index))
                .collect(Collectors.toList()));

        return TypeSpec.classBuilder(cMyBinder)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .superclass(h.cBaseBinder)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(ParameterSpec.builder(h.cClass, "serviceClass").build())
                        .addParameter(ParameterSpec.builder(ClassName.OBJECT, "obj").build())
                        .addStatement("super(serviceClass, obj)")
                        .addCode(binderFunctionCode)
                        .build())
                .build();
    }

    private CodeBlock binderFunctionCode(ExecutableElement method, int index) {
        TypeName sourceType = h.getElementType();
        String methodName = method.getSimpleName().toString();
        String funcName = methodName + "_" + index;
        TypeName returnType = ClassName.get(method.getReturnType());

        CodeBlock functionArgs = IndexedData.stream(method.getParameters())
                .map(param -> {
                    TypeName paramType = ClassName.get(param.data.asType());
                    return CodeBlock.of("($T) args[$L]", paramType, param.index);
                })
                .collect(CodeBlock.joining(", "));

        CodeBlock functionCode = CodeBlock.of("(($T) obj).$L($L)",
                sourceType, methodName, functionArgs);

        if (!TypeName.VOID.equals(returnType)) {
            functionCode = CodeBlock.builder()
                    .addStatement("return ($T) $L", returnType, functionCode)
                    .build();
        } else {
            functionCode = CodeBlock.builder()
                    .addStatement(functionCode)
                    .addStatement("return null")
                    .build();
        }

        TypeSpec MyFunction = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(h.cFunction)
                .addMethod(MethodSpec.methodBuilder("invoke")
                        .addAnnotation(h.cOverride)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(TypeName.OBJECT, "obj")
                        .addParameter(ArrayTypeName.of(TypeName.OBJECT), "args")
                        .returns(TypeName.OBJECT)
                        .addException(h.cThrowable)
                        .addCode(functionCode)
                        .build())
                .build();
        return CodeBlock.of("register($L, $L)", funcName, MyFunction);
    }

    public MethodSpec newBuilderMethod() {
        ClassName cMyBinder = resultClass.nestedClass("MyBinder");
        return MethodSpec.methodBuilder("newBinder")
                .addAnnotation(h.cOverride)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(h.cClass, "serviceClass")
                .addParameter(TypeName.OBJECT, "remoteObject")
                .returns(h.cBinder)
                .addStatement("return new $T(serviceClass, remoteObject)", cMyBinder)
                .build();
    }

    public TypeSpec myProxyType() {
        List<MethodSpec> proxyMethods = IndexedData.stream(methods)
                .map(m -> proxyDataMethod(m.data, m.index))
                .collect(Collectors.toList());

        TypeName elementType = h.getElementType();
        ClassName cMyProxy = resultClass.nestedClass("MyProxy");
        return TypeSpec.classBuilder(cMyProxy)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .superclass(h.cBaseProxy)
                .addSuperinterface(elementType)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(ParameterSpec.builder(h.cClass, "serviceClass").build())
                        .addParameter(ParameterSpec.builder(h.cIBinder, "binder").build())
                        .addStatement("super(serviceClass, binder)")
                        .build())
                .addMethods(proxyMethods)
                .build();
    }

    public MethodSpec proxyDataMethod(ExecutableElement method, int methodIndex) {
        String methodName = method.getSimpleName().toString();
        TypeName returnType = ClassName.get(method.getReturnType());
        String functionIdName = methodName + "_" + methodIndex;

        List<ParameterSpec> proxyMethodParams = IndexedData.stream(method.getParameters())
                .map(param -> param.to(ClassName.get(param.data.asType())))
                .map(paramType -> ParameterSpec.builder(paramType.data, "arg" + paramType.index).build())
                .collect(Collectors.toList());

        CodeBlock proxyMethodInvokeArgCode = IndexedData.stream(method.getParameters())
                .map(param -> param.isFirst()
                        ? CodeBlock.of(", (Object) arg$L", param.index)
                        : CodeBlock.of(", arg$L", param.index)
                )
                .collect(CodeBlock.joining(""));

        CodeBlock proxyMethodInvokeCodes;
        if (!TypeName.VOID.equals(returnType)) {
            proxyMethodInvokeCodes = CodeBlock.of("return ($T) transact(0, $L$L)",
                    returnType, functionIdName, proxyMethodInvokeArgCode);
        } else {
            proxyMethodInvokeCodes = CodeBlock.of("transact($T.FLAG_ONEWAY, $L$L)",
                    h.cIBinder, functionIdName, proxyMethodInvokeArgCode);
        }
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(h.cOverride)
                .addParameters(proxyMethodParams)
                .returns(returnType)
                .addStatement(proxyMethodInvokeCodes)
                .build();
    }

    public MethodSpec newProxyMethod() {
        ClassName cMyProxy = resultClass.nestedClass("MyProxy");
        return MethodSpec.methodBuilder("newProxy")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(h.cClass, "serviceClass")
                .addParameter(h.cIBinder, "binder")
                .returns(TypeName.OBJECT)
                .addAnnotation(h.cOverride)
                .addStatement("return new $T(serviceClass, binder)", cMyProxy)
                .build();
    }
}
