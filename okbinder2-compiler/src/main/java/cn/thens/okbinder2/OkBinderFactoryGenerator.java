package cn.thens.okbinder2;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public final class OkBinderFactoryGenerator {
    private final ProcessingHelper h;
    private final TypeElement element;

    private final List<FieldSpec> factoryFields = new ArrayList<>();
    private final List<MethodSpec> proxyMethods = new ArrayList<>();
    private final CodeBlock.Builder binderFunctionCode = CodeBlock.builder();

    public OkBinderFactoryGenerator(ProcessingHelper h, TypeElement element) {
        this.h = h;
        this.element = element;

        Validate.isTrue(element.getKind().isInterface(), "Not an interface: %s", element.getKind());
    }

    public void generate() {
        AtomicInteger index = new AtomicInteger();
        h.getAllMethods(element).stream()
                .filter(method -> !ElementUtils.isObjectMethod(method) && ElementUtils.isOverridable(method))
                .forEach(method -> buildCodes(method, index.getAndIncrement()));
        h.writeJavaFile(h.getPackageName(element), buildType());
    }

    private void buildCodes(ExecutableElement method, int methodIndex) {
        TypeName MyInterface = ClassName.get(element.asType());
        String methodName = method.getSimpleName().toString();
        TypeName returnType = ClassName.get(method.getReturnType());
        String functionIdName = methodName + "_" + methodIndex;

        List<CodeBlock> functionInvokeArgs = new ArrayList<>();
        List<ParameterSpec> proxyMethodParams = new ArrayList<>();
        CodeBlock.Builder proxyMethodInvokeArgCode = CodeBlock.builder();
        for (VariableElement parameter : method.getParameters()) {
            TypeName ParamType = ClassName.get(parameter.asType());
            int argIndex = functionInvokeArgs.size();
            functionInvokeArgs.add(CodeBlock.of("($T) args[$L]", ParamType, argIndex));
            proxyMethodParams.add(ParameterSpec.builder(ParamType, "arg" + argIndex)
                    .build());
            if (argIndex == 0) {
                proxyMethodInvokeArgCode.add(", (Object) arg" + argIndex);
            } else {
                proxyMethodInvokeArgCode.add(", arg" + argIndex);
            }
        }

        CodeBlock functionInvokeCode = CodeBlock.of("(($T) obj).$L($L)",
                MyInterface, methodName, CodeBlock.join(functionInvokeArgs, ", "));
        CodeBlock proxyMethodInvokeCodes;
        if (!TypeName.VOID.equals(returnType)) {
            functionInvokeCode = CodeBlock.builder()
                    .addStatement("return ($T) $L", returnType, functionInvokeCode)
                    .build();
            proxyMethodInvokeCodes = CodeBlock.of("return ($T) transact(0, $L$L)",
                    returnType, functionIdName, proxyMethodInvokeArgCode.build());
        } else {
            functionInvokeCode = CodeBlock.builder()
                    .addStatement(functionInvokeCode)
                    .addStatement("return null")
                    .build();
            proxyMethodInvokeCodes = CodeBlock.of("transact($T.FLAG_ONEWAY, $L$L)",
                    h.IBinder, functionIdName, proxyMethodInvokeArgCode.build());
        }

        TypeSpec MyFunction = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(h.Function)
                .addMethod(MethodSpec.methodBuilder("invoke")
                        .addAnnotation(h.Override)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(TypeName.OBJECT, "obj")
                        .addParameter(ArrayTypeName.of(TypeName.OBJECT), "args")
                        .returns(TypeName.OBJECT)
                        .addException(h.Throwable)
                        .addCode(functionInvokeCode)
                        .build())
                .build();

//        factoryFields.add(FieldSpec.builder(h.Function, functionIdName + "F")
//                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
//                .initializer(CodeBlock.of("$L", MyFunction))
//                .build());

        binderFunctionCode.addStatement("register($L, $L)", functionIdName, MyFunction);

        factoryFields.add(FieldSpec.builder(h.String, functionIdName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer(CodeBlock.of("$S", CompilerFunctionUtils.getFunctionId(method)))
                .build());

        proxyMethods.add(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(h.Override)
                .addParameters(proxyMethodParams)
                .returns(returnType)
                .addStatement(proxyMethodInvokeCodes)
                .build());
    }

    private TypeSpec buildType() {
        // Factory class
        ClassName MyFactory = ClassName.get(h.getPackageName(element), element.getSimpleName() + "Factory");
        ClassName MyBinder = MyFactory.nestedClass("MyBinder");
        ClassName MyProxy = MyFactory.nestedClass("MyProxy");
        TypeName MyInterface = ClassName.get(element.asType());

        return TypeSpec.classBuilder(MyFactory)
                .addModifiers(Modifier.FINAL)
                .addSuperinterface(h.OkBinderFactory)
                .addMethod(MethodSpec.methodBuilder("newBinder")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(h.Class, "serviceClass")
                        .addParameter(TypeName.OBJECT, "remoteObject")
                        .returns(h.Binder)
                        .addAnnotation(h.Override)
                        .addStatement("return new $T(serviceClass, remoteObject)", MyBinder)
                        .build())
                .addMethod(MethodSpec.methodBuilder("newProxy")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(h.Class, "serviceClass")
                        .addParameter(h.IBinder, "binder")
                        .returns(TypeName.OBJECT)
                        .addAnnotation(h.Override)
                        .addStatement("return new $T(serviceClass, binder)", MyProxy)
                        .build())
                .addFields(factoryFields)
                .addType(TypeSpec.classBuilder(MyProxy)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .superclass(h.BaseProxy)
                        .addSuperinterface(MyInterface)
                        .addMethod(MethodSpec.constructorBuilder()
                                .addParameter(ParameterSpec.builder(h.Class, "serviceClass").build())
                                .addParameter(ParameterSpec.builder(h.IBinder, "binder").build())
                                .addStatement("super(serviceClass, binder)")
                                .build())
                        .addMethods(proxyMethods)
                        .build())
                .addType(TypeSpec.classBuilder(MyBinder)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .superclass(h.BaseBinder)
                        .addMethod(MethodSpec.constructorBuilder()
                                .addParameter(ParameterSpec.builder(h.Class, "serviceClass").build())
                                .addParameter(ParameterSpec.builder(ClassName.OBJECT, "obj").build())
                                .addStatement("super(serviceClass, obj)")
                                .addCode(binderFunctionCode.build())
                                .build())
                        .build())
                .build();
    }
}
