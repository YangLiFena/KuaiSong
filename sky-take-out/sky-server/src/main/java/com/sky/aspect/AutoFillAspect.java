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
@Component
@Slf4j //便于记录日志
public class AutoFillAspect {
    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)") // 处理mapper包下的加入了AutoFill注解的方法
    public void autoFillPointCut(){}

    /**
     * 使用前置通知（前置、后置、环绕）,在通知中对公共字段进行赋值
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
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
