package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现功能字段自动填充处理
 */
@Aspect
@Component // @Component 用于标识一个类为 Spring 组件，将其纳入 Spring IoC 容器中管理
@Slf4j //便于记录日志
public class AutoFillAspect {
    // AOP是基于动态代理的
    // 动态代理：运行期间，对象中方法的动态拦截，在拦截方法的前后执行功能操作
    // 有了动态代理的技术，那么就可以在不修改方法源码的情况下，增强被代理对象的方法的功能，在方法执行前后做任何你想做的事情
    /**
     * 设置切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)") // 处理mapper包下的加入了AutoFill注解的方法
    public void autoFillPointCut(){}

    /**
     * “通知”也称为"增强"
     * 使用前置通知（5中环绕方式：前置通知、后置通知、环绕通知、返回通知、异常通知）,在通知中对公共字段进行赋值
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        // JoinPoint连接点
        log.info("开始进行公共字段自动填充");
        // 获取到当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        // 获取到当前被拦截方法的参数--实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0){
            return;
        }

        Object entity = args[0];
        // 准备赋值数据
        LocalDateTime time = LocalDateTime.now();
        Long empId = BaseContext.getCurrentId();

        // 根据不同的操作类型，为对应的属性通过反射进行赋值
        if (operationType == OperationType.INSERT){
            // 为4个公共字段赋值
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method serUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                // 通过反射为对象属性赋值
                setCreateTime.invoke(entity,time);
                serUpdateTime.invoke(entity,time);
                setCreateUser.invoke(entity,empId);
                setUpdateUser.invoke(entity,empId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if (operationType == OperationType.UPDATE){
            // 为2个公共字段赋值
            try {
                Method serUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                // 通过反射为对象属性赋值
                serUpdateTime.invoke(entity,time);
                setUpdateUser.invoke(entity,empId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
