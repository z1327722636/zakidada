package com.zaki.zakidada.scoring;

import com.zaki.zakidada.model.entity.App;
import com.zaki.zakidada.model.entity.UserAnswer;

import java.util.List;

/**
 * 评分策略接口
 * 评分策略接口，用于根据用户的选择列表和 App 实例计算得分
 * @author zaki
 */
public interface ScoringStrategy {

    /**
     * 执行评分
     *
     * @param choices 用户的选择列表
     * @param app 应用实例
     * @return 评分结果，返回 UserAnswer 实例
     * @throws Exception 评分过程中可能抛出的异常
     */
    UserAnswer doScore(List<String> choices, App app) throws Exception;
}
