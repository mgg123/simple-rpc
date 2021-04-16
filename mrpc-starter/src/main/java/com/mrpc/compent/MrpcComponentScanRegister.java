package com.mrpc.compent;

import com.mrpc.config.annotation.EnableMrpc;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.*;

/**
 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar
 */
public class MrpcComponentScanRegister implements ImportBeanDefinitionRegistrar {

    private static final String BASE_PACKAGES = "basePackages";
    private static final String BASE_PACKAGES_CLASS = "basePackageClasses";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        Set<String> packagesToScan = getPackagesByScan(annotationMetadata);

        registerMrpcServiceAnnotationBeanPostProcessor(packagesToScan, beanDefinitionRegistry);


    }

    //注册MrpcService
    private void registerMrpcServiceAnnotationBeanPostProcessor(Set<String> packagesToScan, BeanDefinitionRegistry beanDefinitionRegistry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MrpcServiceClassPostProcessor.class);
        builder.addConstructorArgValue(packagesToScan);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, beanDefinitionRegistry);
    }

    //扫描注解上的路径
    private Set<String> getPackagesByScan(AnnotationMetadata annotationMetadata) {
        //从注解上获取注解属性
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                annotationMetadata.getAnnotationAttributes(EnableMrpc.class.getName())
        );
        String[] basePackages = attributes.getStringArray(BASE_PACKAGES);
        //按顺序扫描 顺序加入
        if (basePackages == null || basePackages.length == 0) {
            return Collections.singleton(ClassUtils.getPackageName(annotationMetadata.getClassName()));
        }
        Set<String> packagesByScan = new LinkedHashSet<>();
        packagesByScan.addAll(Arrays.asList(basePackages));
        return packagesByScan;
    }
}
