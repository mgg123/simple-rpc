package com.mrpc.config.annotation;

import com.mrpc.compent.MrpcComponentScanRegister;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(MrpcComponentScanRegister.class)
public @interface EnableMrpc {

    /**
     * scan @MrpcService
     * @return
     */
    String[] basePackages() default {};


    Class<?>[] basePackageClasses() default {};

}
