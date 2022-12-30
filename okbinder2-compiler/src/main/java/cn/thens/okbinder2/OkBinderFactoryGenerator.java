package cn.thens.okbinder2;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

public final class OkBinderFactoryGenerator implements TypeElementGenerator {
    private final ProcessingHelper h;
    private final Elements elementUtils;
    private final Filer filer;

    public OkBinderFactoryGenerator(ProcessingHelper h, Elements elementUtils, Filer filer) {
        this.h = h;
        this.elementUtils = elementUtils;
        this.filer = filer;
    }

    @Override
    public void generate(TypeElement element) {
        Validate.isTrue(element.getKind().isInterface(), "Not an interface: %s", element.getKind());
        TypeName MyInterface = ClassName.get(element.asType());
//        System.out.println("## getBinaryName: " + elementUtils.getBinaryName(element));
        String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();

        List<FieldSpec> factoryFields = new ArrayList<>();
        List<MethodSpec> proxyMethods = new ArrayList<>();
        CodeBlock.Builder binderFunctionCode = CodeBlock.builder();

        int methodCount = 0;
        for (Element member : elementUtils.getAllMembers(element)) {
//            System.out.println("## member: " + member + ", " + member.getClass());
            if (shouldSkipMethod(member)) {
                continue;
            }
            ExecutableElement methodMember = (ExecutableElement) member;
            String methodName = methodMember.getSimpleName().toString();
            TypeName returnType = ClassName.get(methodMember.getReturnType());
            String functionIdName = methodName + "_" + methodCount;

            List<CodeBlock> functionInvokeArgs = new ArrayList<>();
            List<ParameterSpec> proxyMethodParams = new ArrayList<>();
            CodeBlock.Builder proxyMethodInvokeArgCode = CodeBlock.builder();
            for (VariableElement parameter : methodMember.getParameters()) {
                TypeName ParamType = ClassName.get(parameter.asType());
                int index = functionInvokeArgs.size();
                functionInvokeArgs.add(CodeBlock.of("($T) args[$L]", ParamType, index));
                proxyMethodParams.add(ParameterSpec.builder(ParamType, "arg" + index)
                        .build());
                if (index == 0) {
                    proxyMethodInvokeArgCode.add(", (Object) arg" + index);
                } else {
                    proxyMethodInvokeArgCode.add(", arg" + index);
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

//            String functionName = functionIdName + "F";
//            factoryFields.add(FieldSpec.builder(t.Function, functionName)
//                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
//                    .initializer(CodeBlock.of("$L", MyFunction))
//                    .build());

            binderFunctionCode.addStatement("register($L, $L)", functionIdName, MyFunction);

            factoryFields.add(FieldSpec.builder(h.String, functionIdName)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(CodeBlock.builder()
                            .add("$S", CompilerFunctionUtils.getFunctionId(methodMember))
                            .build())
                    .build());

            proxyMethods.add(MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(h.Override)
                    .addParameters(proxyMethodParams)
                    .returns(returnType)
                    .addStatement(proxyMethodInvokeCodes)
                    .build());
            methodCount++;
        }

        // Factory class
        ClassName MyFactory = ClassName.get(packageName, getClassName(element) + "Factory");
        ClassName MyBinder = MyFactory.nestedClass("MyBinder");
        ClassName MyProxy = MyFactory.nestedClass("MyProxy");

        TypeSpec MyFactorySpec = TypeSpec.classBuilder(MyFactory)
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

        try {
            JavaFile.builder(packageName, MyFactorySpec)
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String getClassName(TypeElement element) {
        String fileName = elementUtils.getBinaryName(element).toString();
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private static final boolean SHOULD_SKIP_OBJECT_METHODS = true;

    private boolean shouldSkipMethod(Element member) {
        if (!(member instanceof ExecutableElement)) {
            return true;
        }
        for (Modifier modifier : member.getModifiers()) {
            if (modifier == Modifier.PRIVATE || modifier == Modifier.FINAL || modifier == Modifier.STATIC) {
                return true;
            }
        }
        if (!SHOULD_SKIP_OBJECT_METHODS) {
            return false;
        }
        ExecutableElement methodMember = (ExecutableElement) member;
        String methodName = methodMember.getSimpleName().toString();
        List<? extends VariableElement> parameters = methodMember.getParameters();
        if (methodName.equals("toString") && parameters.isEmpty()) {
            return true;
        }
        if (methodName.equals("hashCode") && parameters.isEmpty()) {
            return true;
        }
        if (methodName.equals("equals") && parameters.size() == 1) {
            return ClassName.get(parameters.get(0).asType()).equals(TypeName.OBJECT);
        }
        return false;
    }
}
