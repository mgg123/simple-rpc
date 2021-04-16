package com.mrpc.util;

import org.springframework.cglib.core.ReflectUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class MrpcAnnotationUtils {

    public static <T> T getAttribute(Annotation annotation, String attributeName) {
        return annotation == null ? null : invokeMethod(annotation,attributeName);
    }

    private static <T> T invokeMethod(Object object, String attributeName,Object... methodParameters) {
        Class type = object.getClass();
        Class[] parameterTypes = resolveTypes(methodParameters);
        Method method = findMethod(type, attributeName, parameterTypes);
        T value = null;

        try {
            method.setAccessible(true);
            value = (T) method.invoke(object, methodParameters);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return value;
    }

    private static Method findMethod(Class type, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        try {
            if(type != null && !StringUtils.isEmpty(methodName)) {
                method = type.getDeclaredMethod(methodName,parameterTypes);
            }
        } catch (NoSuchMethodException e)  {

        }
        return method;
    }

    private static Class[] resolveTypes(Object[] methodParameters) {
        if(methodParameters == null || methodParameters.length == 0) {
            return new Class[0];
        }
        int size = methodParameters.length;
        Class[] types = new Class[size];
        for (int i = 0; i < size; i++) {
            Object value = methodParameters[i];
            types[i] = value == null ? null : value.getClass();
        }

        return types;
    }


}
