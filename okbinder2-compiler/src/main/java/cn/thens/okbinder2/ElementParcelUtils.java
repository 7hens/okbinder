package cn.thens.okbinder2;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.Locale;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

final class ElementParcelUtils {
    private static final String JAVA_LANG = "java.lang.";
    private static final String PARCELABLE = "Parcelable";

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
