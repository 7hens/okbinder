package cn.thens.okbinder2;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

final class ParcelCodeHelper {
    private static final String JAVA_LANG = "java.lang.";
    private static final String PARCELABLE = "Parcelable";

    private final ProcessingHelper h;

    ParcelCodeHelper(ProcessingHelper h) {
        this.h = h;
    }

    public CodeBlock read(TypeMirror type) {
        String readMethodName = getReadMethodNameForType(type);
        if ("readParcelable".equals(readMethodName)) {
            return CodeBlock.of("$L(getClass().getClassLoader())", readMethodName);
        }
        if ("createTypedArray".equals(readMethodName)) {
            TypeMirror componentType = ((ArrayType) type).getComponentType();
            return CodeBlock.of("$L($T.CREATOR)", readMethodName, TypeName.get(componentType));
        }
        return CodeBlock.of("$L()", readMethodName);
    }

    public static String getWriteMethodNameForType(TypeMirror type) {
        return "write" + getDataTypeNameForType(type);
    }

    public static String getReadMethodNameForType(TypeMirror type) {
        String dataType = getDataTypeNameForType(type);
        if (dataType.endsWith("Array")) {
            return "create" + dataType;
        }
        return "read" + dataType;
    }

    private static String getDataTypeNameForType(TypeMirror type) {
        TypeKind kind = type.getKind();
        if (kind.isPrimitive()) {
            return StringUtils.capitalize(kind.name().toLowerCase());
        }
        String className = type.toString();
        if (className.startsWith(JAVA_LANG)) {
            return StringUtils.capitalize(className.substring(JAVA_LANG.length()));
        }
        if (type.getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) type;
            TypeMirror componentType = arrayType.getComponentType();
            String componentTypeName = getDataTypeNameForType(componentType);
            if (PARCELABLE.equals(componentTypeName)) {
                return "TypedArray";
            }
            return componentTypeName + "Array";
        }
        return PARCELABLE;
    }
}
