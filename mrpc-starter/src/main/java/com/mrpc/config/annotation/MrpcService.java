package com.mrpc.config.annotation;

import java.lang.annotation.*;

/**
 * 生产者服务注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited //可被继承
public @interface MrpcService {

}
