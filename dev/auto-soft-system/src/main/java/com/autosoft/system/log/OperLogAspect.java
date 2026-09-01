package com.autosoft.system.log;

import com.autosoft.framework.log.OperLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 切 @OperLog，失败也记一条。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Aspect
@Component
public class OperLogAspect {

    private final OperLogManager operLogManager;

    public OperLogAspect(OperLogManager operLogManager) {
        this.operLogManager = operLogManager;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        String bizId = firstId(joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            if (bizId == null && result instanceof Long id) {
                bizId = String.valueOf(id);
            }
            operLogManager.write(operLog.module(), operLog.action(), bizId, true,
                    (int) (System.currentTimeMillis() - start), joinPoint.getArgs());
            return result;
        } catch (Throwable ex) {
            operLogManager.write(operLog.module(), operLog.action(), bizId, false,
                    (int) (System.currentTimeMillis() - start),
                    ((MethodSignature) joinPoint.getSignature()).getMethod().getName() + ":" + ex.getMessage());
            throw ex;
        }
    }

    private String firstId(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Long id) {
                return String.valueOf(id);
            }
        }
        return null;
    }
}
