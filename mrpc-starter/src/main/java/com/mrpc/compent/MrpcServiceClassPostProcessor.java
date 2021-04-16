package com.mrpc.compent;

import com.mrpc.util.MrpcAnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;

import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;

/**
 * MrpcService进行注册为spring bean
 * @See BeanDefinitionRegistryPostProcessor
 * 注册beanDefinition时候触发方法 postProcessBeanDefinitionRegistry()
 */
public class MrpcServiceClassPostProcessor implements
        BeanDefinitionRegistryPostProcessor, EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static List<Class<? extends Annotation>> serviceAnnotationTypes = new ArrayList(){
        {
            add("MrpcService");
        }
    };

    protected final Set<String> packagesToScan;

    private Environment environment;

    private ResourceLoader resourceLoader;

    private ClassLoader classLoader;

    public MrpcServiceClassPostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    public MrpcServiceClassPostProcessor(Collection<String> packagesToScan) {
        this(new LinkedHashSet<>(packagesToScan));
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        //可通过applicationListener 进行注册完beanDefinition后 实例化bean;
        //TODO

        //获取扫描的路径
        Set<String> resolvedPackagesToScan = resolvePackagesToScan(packagesToScan);

        //
        if(!CollectionUtils.isEmpty(resolvedPackagesToScan)) {
            registerServiceBeans(resolvedPackagesToScan,beanDefinitionRegistry);
        } else {
            if(logger.isWarnEnabled()) {
                logger.warn("packagesToScan is empty , rpcBean registry will be ignored!");
            }
        }

    }

    //为每一个带有@Mrpc的类 注册serviceBean
    private void registerServiceBeans(Set<String> resolvedPackagesToScan, BeanDefinitionRegistry beanDefinitionRegistry) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanDefinitionRegistry,
                false,environment,resourceLoader);

        //设置beanNaneGenerator
        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(beanDefinitionRegistry);
        scanner.setBeanNameGenerator(resolveBeanNameGenerator(beanDefinitionRegistry));
        //scan扫描带有mrpc注解的类
        serviceAnnotationTypes.forEach(an-> scanner.addIncludeFilter(new AnnotationTypeFilter(an)));

        //开始进行注册
        for(String scan : resolvedPackagesToScan) {
            //扫描注册
            scanner.scan(scan);
            // Finds all BeanDefinitionHolders of @Service whether @ComponentScan scans or not.
            Set<BeanDefinitionHolder> holderSet = findServiceBeanDefinitionHolders(scanner,scan,beanDefinitionRegistry,beanNameGenerator);
            
            if(!CollectionUtils.isEmpty(holderSet)) {
                for(BeanDefinitionHolder holder : holderSet) {
                    registerServiceBean(holder,beanDefinitionRegistry,scanner);    
                }
                if (logger.isInfoEnabled()) {
                    logger.info(holderSet.size() + " annotated Dubbo's @Service Components { " +
                            holderSet +
                            " } were scanned under package[" + scan + "]");
                }
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("No Spring Bean annotating Dubbo's @Service was found under package["
                            + scan + "]");
                }
            }
        }

    }

    private void registerServiceBean(BeanDefinitionHolder beanDefinitionHolder, BeanDefinitionRegistry beanDefinitionRegistry, ClassPathBeanDefinitionScanner scanner) {
        Class<?> beanClass = resolveClass(beanDefinitionHolder);
        Annotation service = findServiceAnnotation(beanClass);

        //获取属性
        AnnotationAttributes attributes = getAnnotationAttributes(service,false,false);

        Class<?> interfaceClass = resolveServiceInterfaceClass(attributes, beanClass);

        String annotatedServiceBeanName = beanDefinitionHolder.getBeanName();

        AbstractBeanDefinition serviceBeanDefinition =
                buildServiceBeanDefinition(service, serviceAnnotationAttributes, interfaceClass, annotatedServiceBeanName);

        // ServiceBean Bean name
        String beanName = generateServiceBeanName(serviceAnnotationAttributes, interfaceClass);

        if (scanner.checkCandidate(beanName, serviceBeanDefinition)) { // check duplicated candidate bean
            registry.registerBeanDefinition(beanName, serviceBeanDefinition);

            if (logger.isInfoEnabled()) {
                logger.info("The BeanDefinition[" + serviceBeanDefinition +
                        "] of ServiceBean has been registered with name : " + beanName);
            }

        } else {

            if (logger.isWarnEnabled()) {
                logger.warn("The Duplicated BeanDefinition[" + serviceBeanDefinition +
                        "] of ServiceBean[ bean name : " + beanName +
                        "] was be found , Did @DubboComponentScan scan to same package in many times?");
            }

        }
    
    }

    private Class<?> resolveServiceInterfaceClass(AnnotationAttributes attributes, Class<?> beanClass) {

        //Class intefaceClass = AnnotationUtils.get(attributes,"interfaceClass");


    }

    private Annotation findServiceAnnotation(Class<?> beanClass) {
        return serviceAnnotationTypes
                .stream()
                .map(annotationType -> AnnotatedElementUtils.findMergedAnnotation(beanClass, annotationType))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Class<?> resolveClass(BeanDefinitionHolder holder) {
        BeanDefinition beanDefinition = holder.getBeanDefinition();
        String beanClassName = beanDefinition.getBeanClassName();
        return ClassUtils.resolveClassName(beanClassName,classLoader);
    }

    private Set<BeanDefinitionHolder> findServiceBeanDefinitionHolders(ClassPathBeanDefinitionScanner scanner, String scan, BeanDefinitionRegistry beanDefinitionRegistry, BeanNameGenerator beanNameGenerator) {
        //查询所有候选者
        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(scan);
        //BeanDefinitionHolder持有
        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet(beanDefinitions.size());

        beanDefinitions.forEach(p -> {
            String beanName = beanNameGenerator.generateBeanName(p,beanDefinitionRegistry);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(p,beanName);
            beanDefinitionHolders.add(beanDefinitionHolder);
        });
        return  beanDefinitionHolders;
    }

    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry beanDefinitionRegistry) {
        BeanNameGenerator beanNameGenerator = null;
        //获取自定义beanNameGenerator
        if(beanDefinitionRegistry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry singletonBeanRegistry = (SingletonBeanRegistry) beanDefinitionRegistry;
            beanNameGenerator = (BeanNameGenerator) singletonBeanRegistry.getSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
        }
        if(beanNameGenerator == null) {
            beanNameGenerator = new AnnotationBeanNameGenerator();
        }
        return beanNameGenerator;
    }

    /**
     * 数据格式
     * @param packagesToScan
     * @return
     */
    private Set<String> resolvePackagesToScan(Set<String> packagesToScan) {
        Set<String> resolvedPackagesToScan = new LinkedHashSet<>(packagesToScan.size());
        for(String scan : packagesToScan) {
            if(StringUtils.hasText(scan)) {
                String resolvedPackageToScan = environment.resolvePlaceholders(scan.trim());
                resolvedPackagesToScan.add(resolvedPackageToScan);
            }
        }
        return resolvedPackagesToScan;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
