package com.zaki.zakidada.scoring;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 评分策略配置
 * @author 13277
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ScoringStrategyConfig {

    /**
     * 应用类型
     * @return
     */
    int appType();

    /**
     * 评分策略
     * @return
     */
    int scoringStrategy();
}