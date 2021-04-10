package com.mrpc.config.annotation;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited //可被继承
public @interface MrpcRef {

    /**
     * 调用url
     */
    String url();

}
