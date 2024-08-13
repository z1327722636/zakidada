
package com.zaki.zakidada.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zaki.zakidada.constant.AiSystemMessage;
import com.zaki.zakidada.manager.AiManager;
import com.zaki.zakidada.model.dto.question.QuestionAnswerDTO;
import com.zaki.zakidada.model.dto.question.QuestionContentDTO;
import com.zaki.zakidada.model.entity.App;
import com.zaki.zakidada.model.entity.Question;
import com.zaki.zakidada.model.entity.UserAnswer;
import com.zaki.zakidada.model.enums.AppTypeEnum;
import com.zaki.zakidada.model.vo.QuestionVO;
import com.zaki.zakidada.service.QuestionService;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


/**
 * @author 13277
 */

@ScoringStrategyConfig(appType = 0, scoringStrategy = 1)
public class AiScoreScoringStrategy implements ScoringStrategy {
        @Resource
        private QuestionService questionService;


        @Resource
        private AiManager aiManager;

        @Override
        public UserAnswer doScore(List<String> choices, App app) throws Exception {
            Long appId = app.getId();
            // 1. 根据 id 查询到题目和题目结果信息
            Question question = questionService.getOne(
                    Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
            );
            String userMessage = getGenerateQuestionUserMessage(app, choices, question);
            // 2. 调用aiManager的接口，获取结果

            String res = aiManager.doSyncUnstableRequest(AiSystemMessage.AI_SCORING_SCORING_SYSTEM_MESSAGE, userMessage);
            //解析返回
            int start = res.indexOf("{");
            int end = res.lastIndexOf("}");
            String json = res.substring(start, end + 1);
            // 3. 构造返回值，填充答案对象的属性
            UserAnswer userAnswer = null;
            try {
                userAnswer = JSONUtil.toBean(json, UserAnswer.class);
            } catch (Exception e) {
                // 处理解析错误，例如记录日志或抛出自定义异常
                throw new RuntimeException("解析JSON失败，错误信息：" + e.getMessage());
            }
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(JSONUtil.toJsonStr(choices));
            return userAnswer;

        }

        private String getGenerateQuestionUserMessage(App app, List<String> choices, Question question) {
            StringBuilder userMessage = new StringBuilder();
            userMessage.append(app.getAppName() + "，").append('\n');
            userMessage.append(app.getAppDesc() + "，").append('\n');
            userMessage.append(AppTypeEnum.getEnumByValue(app.getAppType()).getText() + "，").append('\n');
            QuestionVO questionVO = QuestionVO.objToVo(question);
            List<QuestionContentDTO> questionContentDTOList = questionVO.getQuestionContent();
            List<QuestionAnswerDTO> questionAnswerDTOList = getQuestionAnswerDTOS(choices, questionContentDTOList);
            userMessage.append(JSONUtil.toJsonStr(questionAnswerDTOList));
            return userMessage.toString();
        }

        @NotNull
        private static List<QuestionAnswerDTO> getQuestionAnswerDTOS(List<String> choices, List<QuestionContentDTO> questionContentDTOList) {
            List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
            for (int i = 0; i < questionContentDTOList.size(); i++) {
                QuestionAnswerDTO questionAnswerDTO = new QuestionAnswerDTO();
                questionAnswerDTO.setTitle(questionContentDTOList.get(i).getTitle());
                for (QuestionContentDTO.Option option : questionContentDTOList.get(i).getOptions()) {
                    // 如果答案和选项的key匹配
                    if (option.getKey().equals(choices.get(i))) {
                        // 获取选项的result属性
                        String value = option.getValue();
                        questionAnswerDTO.setUserAnswer(value);
                        questionAnswerDTO.setOptionScore(option.getScore());
                    }
                }
                questionAnswerDTOList.add(questionAnswerDTO);
            }
            return questionAnswerDTOList;
        }
    }



