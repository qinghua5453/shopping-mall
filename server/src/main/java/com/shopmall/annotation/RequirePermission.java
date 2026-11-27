package com.shopmall.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明接口所需权限码，由 PermissionInterceptor 校验。
 * 未标注解的管理端方法默认为「已登录即可访问」。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] value();

    Logical logical() default Logical.AND;
}
