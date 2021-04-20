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
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

public class OkBinderFactoryGenerator {
    String binderPackageName = "android.os";
    ClassName cBinder = ClassName.get(binderPackageName, "Binder");
    ClassName cIBinder = ClassName.get(binderPackageName, "IBinder");
    ClassName cIInterface = ClassName.get(binderPackageName, "IInterface");
    ClassName cParcel = ClassName.get(binderPackageName, "Parcel");
    ClassName cRemoteException = ClassName.get(binderPackageName, "RemoteException");
    ClassName cClassLoader = ClassName.get("java.lang", "ClassLoader");
    ClassName cOkBinder = ClassName.get("cn.thens.okbinder", "OkBinder");
    ClassName cMyParcel = ClassName.get("cn.thens.okbinder", "OkBinderParcel");
    ClassName cString = ClassName.get(String.class);
    String remoteObject = "remoteObject";
    String descriptor = "DESCRIPTOR";
    String classLoader = "classLoader";
    String result = "result";
    String reply = "reply";
    String binder = "binder";

    private final Elements elements;
    private final Filer filer;

    public OkBinderFactoryGenerator(Elements elements, Filer filer) {
        this.elements = elements;
        this.filer = filer;
    }

    public void generate(TypeElement element) {
        TypeName cMyInterface = ClassName.get(element.asType());
        System.out.println("## getBinaryName: " + elements.getBinaryName(element));
        String packageName = elements.getPackageOf(element).getQualifiedName().toString();
        String myBinderClass = elements.getBinaryName(element).toString();
        myBinderClass = myBinderClass.substring(myBinderClass.lastIndexOf(".") + 1) + "Binder";
        TypeName cMyBinder = ClassName.get(packageName, myBinderClass);
        TypeName cMyProxy = ClassName.get(packageName, myBinderClass, "Proxy");

        List<MethodSpec> proxyMethods = new ArrayList<>();

        CodeBlock.Builder onTransactCodes = CodeBlock.builder()
                .beginControlFlow("try")
                .addStatement("data.enforceInterface($L)", descriptor)
                .addStatement("$T $L = $L.getClass().getClassLoader()",
                        cClassLoader, classLoader, remoteObject)
                .addStatement("$T $L = null", TypeName.OBJECT, result);

        int methodCount = 0;
        List<FieldSpec> transactionFields = new ArrayList<>();
        onTransactCodes.beginControlFlow("switch (code)");

        for (Element member : elements.getAllMembers(element)) {
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

                String transactionCode = methodName + "_" + "T" + methodCount;
                transactionFields.add(FieldSpec.builder(TypeName.INT, transactionCode)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("IBinder.FIRST_CALL_TRANSACTION + " + methodCount)
                        .build());

                onTransactCodes.beginControlFlow("case $L:", transactionCode);

                CodeBlock.Builder proxyMethodCodes = CodeBlock.builder()
                        .addStatement("$T data = $T.obtain()", cParcel, cParcel)
                        .addStatement("$T reply = $T.obtain()", cParcel, cParcel)
                        .addStatement("$T result = null", TypeName.OBJECT)
                        .addStatement("$T classLoader = $T.class.getClassLoader()", cClassLoader, cMyInterface)
                        .beginControlFlow("try")
                        .addStatement("data.writeInterfaceToken($L)", descriptor);

                List<CodeBlock> args = new ArrayList<>();
                List<ParameterSpec> proxyMethodParams = new ArrayList<>();
                for (VariableElement parameter : methodMember.getParameters()) {
                    TypeName cParamType = ClassName.get(parameter.asType());
                    String argName = "arg" + args.size();
                    onTransactCodes.addStatement("$T $L = ($T) $T.readValue(data, $L)",
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
                    onTransactCodes.addStatement(invokeMethod);
                    transactFlag = CodeBlock.of("$T.FLAG_ONEWAY", cIBinder);
                    proxyReturnCode = CodeBlock.of("");
                } else {
                    onTransactCodes.addStatement("$L = $L", result, invokeMethod);
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

                onTransactCodes
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

        onTransactCodes.endControlFlow() // end switch (code)
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

        // Proxy class as inner class of Binder class
        TypeSpec proxyClassSpec = TypeSpec.classBuilder("Proxy")
                .addModifiers(Modifier.STATIC)
                .addSuperinterface(cMyInterface)
                .addField(FieldSpec.builder(cIBinder, "binder", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(ParameterSpec.builder(cIBinder, "binder").build())
                        .addStatement("this.binder = binder")
                        .build())
                .addMethods(proxyMethods)
                .build();

        // AIDL Binder class
        TypeSpec myBinderClassSpec = TypeSpec.classBuilder(myBinderClass)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(cBinder)
                .addSuperinterface(cIInterface)
                .addType(proxyClassSpec)
                .addField(MyBinder_DESCRIPTOR(cMyInterface))
                .addFields(transactionFields)
                .addField(cMyInterface, remoteObject, Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MyBinder_constructor(cMyInterface))
                .addMethod(MyBinder_asBinder())
                .addMethod(MyBinder_onTransact(onTransactCodes.build()))
                .addMethod(MethodSpec.methodBuilder("create")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(ParameterSpec.builder(cMyInterface, remoteObject).build())
                        .returns(cBinder)
                        .addStatement("return new $T($L)", cMyBinder, remoteObject)
                        .build())
                .addMethod(MethodSpec.methodBuilder("proxy")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(ParameterSpec.builder(cIBinder, binder).build())
                        .returns(cMyInterface)
                        .addStatement("return new $T($L)", cMyProxy, binder)
                        .build())
                .build();

        try {
            JavaFile.builder(packageName, myBinderClassSpec)
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private FieldSpec MyBinder_DESCRIPTOR(TypeName cMyInterface) {
        return FieldSpec.builder(cString, descriptor)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", cMyInterface)
                .build();
    }

    private MethodSpec MyBinder_constructor(TypeName cMyInterface) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ParameterSpec.builder(cMyInterface, remoteObject).build())
                .addStatement("this.$L = $L", remoteObject, remoteObject)
                .addStatement("attachInterface(this, $L)", descriptor)
                .build();
    }

    private MethodSpec MyBinder_asBinder() {
        return MethodSpec.methodBuilder("asBinder")
                .addModifiers(Modifier.PUBLIC)
                .returns(cIBinder)
                .addStatement("return this")
                .build();
    }

    private MethodSpec MyBinder_onTransact(CodeBlock codeBlock) {
        return MethodSpec.methodBuilder("onTransact")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.INT, "code")
                .addParameter(cParcel, "data")
                .addParameter(cParcel, "reply")
                .addParameter(TypeName.INT, "flags")
                .returns(TypeName.BOOLEAN)
                .addException(cRemoteException)
                .addCode(codeBlock)
                .addStatement("return true")
                .build();
    }
}
