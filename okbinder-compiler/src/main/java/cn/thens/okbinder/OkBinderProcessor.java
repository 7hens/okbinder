package cn.thens.okbinder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

public class OkBinderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        String binderPackageName = "android.os";
        ClassName cBinder = ClassName.get(binderPackageName, "Binder");
        ClassName cIBinder = ClassName.get(binderPackageName, "IBinder");
        ClassName cIInterface = ClassName.get(binderPackageName, "IInterface");
        ClassName cParcel = ClassName.get(binderPackageName, "Parcel");
        ClassName cRemoteException = ClassName.get(binderPackageName, "RemoteException");
        ClassName cClassLoader = ClassName.get("java.lang", "ClassLoader");
        ClassName cOkBinder = ClassName.get("cn.thens.okbinder", "OkBinder");
        ClassName cMyParcel = cOkBinder.nestedClass("MyParcel");
        ClassName cString = ClassName.get(String.class);
        String remoteObject = "remoteObject";
        String descriptor = "DESCRIPTOR";
        String classLoader = "classLoader";
        String result = "result";
        String reply = "reply";
        String binder = "binder";

        Elements elementUtils = processingEnv.getElementUtils();

        Set<? extends Element> elements = env.getElementsAnnotatedWith(OkBinderService.class);
        for (Element element : elements) {
            TypeName serviceType = ClassName.get(element.asType());
            TypeElement typeElement = (TypeElement) element;
            System.out.println("## getBinaryName: " + elementUtils.getBinaryName(typeElement));
            String serviceClassName = elementUtils.getBinaryName(typeElement).toString();
            serviceClassName = serviceClassName.substring(serviceClassName.lastIndexOf(".") + 1) + "_Service";

            List<MethodSpec> proxyMethods = new ArrayList<>();

            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addParameter(ParameterSpec.builder(serviceType, remoteObject).build())
                    .addStatement("this.$L = $L", remoteObject, remoteObject)
                    .addStatement("attachInterface(this, $L)", descriptor)
                    .build();

            FieldSpec descriptorField = FieldSpec.builder(cString, descriptor)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", serviceType)
                    .build();

            MethodSpec asBinderMethod = MethodSpec.methodBuilder("asBinder")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(cIBinder)
                    .addStatement("return this")
                    .build();

            CodeBlock.Builder transactionCodes = CodeBlock.builder()
                    .beginControlFlow("try")
                    .addStatement("data.enforceInterface($L)", descriptor)
                    .addStatement("$T $L = $L.getClass().getClassLoader()",
                            cClassLoader, classLoader, remoteObject)
                    .addStatement("$T $L = null", TypeName.OBJECT, result);

            int methodCount = 0;
            List<FieldSpec> transactionFields = new ArrayList<>();
            transactionCodes.beginControlFlow("switch (code)");

            for (Element member : elementUtils.getAllMembers(typeElement)) {
                System.out.println("## member: " + member + ", " + member.getClass());
                if (member instanceof ExecutableElement) {
                    boolean shouldSkip = false;
                    for (Modifier modifier : member.getModifiers()) {
                        if (modifier == Modifier.FINAL || modifier == Modifier.STATIC) {
                            shouldSkip = true;
                            break;
                        }
                    }
                    if (shouldSkip) {
                        continue;
                    }
                    ExecutableElement methodMember = (ExecutableElement) member;
                    String methodName = methodMember.getSimpleName().toString();

                    String transactionCode = "TRANSACTION_" + methodCount + "_" + methodName;
                    transactionFields.add(FieldSpec.builder(TypeName.INT, transactionCode)
                            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer("IBinder.FIRST_CALL_TRANSACTION + " + methodCount)
                            .build());

                    transactionCodes.beginControlFlow("case $L:", transactionCode);

                    CodeBlock.Builder proxyMethodCodes = CodeBlock.builder()
                            .addStatement("$T data = $T.obtain()", cParcel, cParcel)
                            .addStatement("$T reply = $T.obtain()", cParcel, cParcel)
                            .addStatement("$T result = null", TypeName.OBJECT)
                            .addStatement("$T classLoader = $T.class.getClassLoader()", cClassLoader, serviceType)
                            .beginControlFlow("try")
                            .addStatement("data.writeInterfaceToken($L)", descriptor);

                    List<CodeBlock> args = new ArrayList<>();
                    List<ParameterSpec> proxyMethodParams = new ArrayList<>();
                    for (VariableElement parameter : methodMember.getParameters()) {
                        TypeName cParamType = ClassName.get(parameter.asType());
                        String argName = "arg" + args.size();
                        transactionCodes.addStatement("$T $L = ($T) $T.readValue(data, $L)",
                                cParamType, argName, cParamType, cMyParcel, classLoader);
                        args.add(CodeBlock.of("$L", argName));
                        proxyMethodParams.add(ParameterSpec.builder(cParamType, argName).build());
                        proxyMethodCodes.addStatement("$L.writeValue(data, $L)", cMyParcel, argName);
                    }
                    CodeBlock invokeMethod = CodeBlock.of("$L.$L($L)",
                            remoteObject, methodName, CodeBlock.join(args, ", "));


                    CodeBlock transactFlag;
                    CodeBlock proxyReturnCode;
                    TypeName returnType = ClassName.get(methodMember.getReturnType());
                    if (TypeName.VOID.equals(returnType)) {
                        transactionCodes.addStatement(invokeMethod);
                        transactFlag = CodeBlock.of("$T.FLAG_ONEWAY", cIBinder);
                        proxyReturnCode = CodeBlock.of("");
                    } else {
                        transactionCodes.addStatement("$L = $L", result, invokeMethod);
                        transactFlag = CodeBlock.of("0");
                        proxyReturnCode = CodeBlock.of("($T) result", returnType);
                    }

                    proxyMethodCodes
                            .addStatement("binder.transact($L, data, reply, $L)", transactionCode, transactFlag)
                            .addStatement("reply.readException()")
                            .beginControlFlow("if (reply.readInt() != 0)")
                            .addStatement("result = $T.readValue(reply, classLoader)", cMyParcel)
                            .endControlFlow()
                            .nextControlFlow("catch (Throwable e)")
                            .addStatement("throw new RuntimeException(e)")
                            .nextControlFlow("finally")
                            .addStatement("reply.recycle()")
                            .addStatement("data.recycle()")
                            .endControlFlow()
                            .addStatement("return $L", proxyReturnCode);

                    transactionCodes
                            .addStatement("break")
                            .endControlFlow(); // end case x:


                    proxyMethods.add(MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC)
                            .addParameters(proxyMethodParams)
                            .returns(returnType)
                            .addCode(proxyMethodCodes.build())
                            .build());

                    methodCount++;
                }
            }

            transactionCodes.endControlFlow() // end switch (code)
                    .beginControlFlow("if ($L != null)", reply)
                    .addStatement("$L.writeNoException()", reply)
                    .beginControlFlow("if ($L != null)", result)
                    .addStatement("$L.writeInt(1)", reply)
                    .addStatement("$L.writeValue($L, $L)", cMyParcel, reply, result)
                    .nextControlFlow("else")
                    .addStatement("$L.writeInt(0)", reply)
                    .endControlFlow() // end if (result != null)
                    .endControlFlow() // end if (reply != null)
                    .nextControlFlow("catch (Throwable e)") // end try
                    .addStatement("$T remoteException = new $T()", cRemoteException, cRemoteException)
                    .addStatement("remoteException.initCause(e)")
                    .addStatement("throw remoteException")
                    .endControlFlow();

            MethodSpec onTransactMethod = MethodSpec.methodBuilder("onTransact")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.INT, "code")
                    .addParameter(cParcel, "data")
                    .addParameter(cParcel, "reply")
                    .addParameter(TypeName.INT, "flags")
                    .returns(TypeName.BOOLEAN)
                    .addException(cRemoteException)
                    .addCode(transactionCodes.build())
                    .addStatement("return true")
                    .build();

            TypeSpec proxyClassSpec = TypeSpec.classBuilder("Proxy")
                    .addModifiers(Modifier.STATIC)
                    .addSuperinterface(serviceType)
                    .addField(FieldSpec.builder(cIBinder, "binder", Modifier.PRIVATE, Modifier.FINAL).build())
                    .addMethod(MethodSpec.constructorBuilder()
                            .addParameter(ParameterSpec.builder(cIBinder, "binder").build())
                            .addStatement("this.binder = binder")
                            .build())
                    .addMethods(proxyMethods)
                    .build();

            TypeSpec serviceClassSpec = TypeSpec.classBuilder(serviceClassName)
                    .superclass(cBinder)
                    .addSuperinterface(cIInterface)
                    .addType(proxyClassSpec)
                    .addField(descriptorField)
                    .addFields(transactionFields)
                    .addField(serviceType, remoteObject, Modifier.PRIVATE, Modifier.FINAL)
                    .addMethod(constructor)
                    .addMethod(asBinderMethod)
                    .addMethod(onTransactMethod)
                    .build();


            String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
            try {
                JavaFile.builder(packageName, serviceClassSpec)
                        .indent("    ")
                        .build()
                        .writeTo(processingEnv.getFiler());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(OkBinderService.class.getCanonicalName());
    }
}