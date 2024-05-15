package com.sky.annotation;


import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解作为切点，用于标识某个方法需要进行功能字段自动填充处理
 */
@Target(ElementType.METHOD) // 指定注解的作用目标，即可以标注在哪些类型的元素上
@Retention(RetentionPolicy.RUNTIME) // @Retention 注解用于指定注解的保留策略，即注解在何时有效, RetentionPolicy.RUNTIME 表示在运行时有效，这意味着这个注解在编译后仍然可以通过反射机制被读取到
public @interface AutoFill {
    // 数据库操作类型：UPDATE, INSERT
    OperationType value(); //  value 自定义注解参数名
}
