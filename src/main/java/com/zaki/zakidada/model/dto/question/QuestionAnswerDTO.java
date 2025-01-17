package com.zaki.zakidada.model.dto.question;

import lombok.Data;

/**
 * 题目答案封装类（用于 AI 评分）
 *
* @author <a href="https://github.com/1327722636">zaki</a>
 */
@Data
public class QuestionAnswerDTO {

    /**
     * 题目
     */
    private String title;

    /**
     * 用户答案
     */
    private String userAnswer;

    /**
     * 选项得分
     */
    private Integer optionScore;
}
