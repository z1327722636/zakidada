package com.zaki.zakidada.service.impl;

import cn.hutool.core.collection.CollUtil;

import cn.hutool.json.JSONException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zaki.zakidada.common.ErrorCode;
import com.zaki.zakidada.constant.AiSystemMessage;
import com.zaki.zakidada.constant.CommonConstant;
import com.zaki.zakidada.exception.BusinessException;
import com.zaki.zakidada.exception.ThrowUtils;
import com.zaki.zakidada.manager.AiManager;
import com.zaki.zakidada.mapper.QuestionMapper;
import com.zaki.zakidada.model.dto.question.QuestionQueryRequest;
import com.zaki.zakidada.model.entity.App;
import com.zaki.zakidada.model.entity.Question;
import com.zaki.zakidada.model.entity.User;
import com.zaki.zakidada.model.vo.QuestionVO;
import com.zaki.zakidada.model.vo.UserVO;
import com.zaki.zakidada.service.AppService;
import com.zaki.zakidada.service.QuestionService;
import com.zaki.zakidada.service.UserService;
import com.zaki.zakidada.utils.SqlUtils;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 题目服务实现
 *
 * @author <a href="https://github.com/1327722636">zaki</a>
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private UserService userService;
    @Resource
    private AppService appService;

    @Resource
    private AiManager  aiManager;

    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        //  从对象中取值
        String questionContent = question.getQuestionContent();
        Long appId = question.getAppId();

        // 创建数据时，参数不能为空
        if (add) {
            // 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(questionContent), ErrorCode.PARAMS_ERROR, "题目内容不能为空");
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 非法");
        }
        // 修改数据时，有参数则校验
        // 补充校验规则
        if (appId != null) {
            App app = appService.getById(appId);
            ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        //  从对象中取值
        Long id = questionQueryRequest.getId();
        String questionContent = questionQueryRequest.getQuestionContent();
        Long appId = questionQueryRequest.getAppId();
        Long userId = questionQueryRequest.getUserId();
        Long notId = questionQueryRequest.getNotId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();


        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(questionContent), "questionContent", questionContent);
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(appId), "appId", appId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);
        // endregion

        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    @Override

    /**
     * 生成题目标题
     *
     * @param number 题目数量
     * @return 生成的题目标题 JSON 字符串
     */
    public String AICreateTitle(int number, String appAiSysMessageConfig, String appAiUserMessageConfig) {

        // 存储 AI 生成的所有题目
        List<ChatMessage> assistantChatMessageList = new ArrayList<>();

        // 按每次最多生成 10 道题目的方式调用 AI，直到生成完所有题目
        while (number >= 10) {
            invokeAiLoop(number, appAiSysMessageConfig, appAiUserMessageConfig, assistantChatMessageList);
            number = number - 10;
        }
        if (number > 0) {
            invokeAiLoop(number, appAiSysMessageConfig, appAiUserMessageConfig, assistantChatMessageList);
        }
        //将结果合并成一个 List 返回
        JSONArray result = new JSONArray();
        for (ChatMessage message : assistantChatMessageList) {
            String content = String.valueOf(message.getContent());
            try {
                JSONArray jsonArray = JSONArray.parseArray(content);
                result.addAll(jsonArray);
            } catch (JSONException e) {
                // 处理解析错误，例如记录日志或跳过该条数据
                System.err.println("解析JSON出错，跳过该条数据: " + content);
            }
        }
        return JSON.toJSONString(result);
    }

    /**
     * 调用 AI 生成题目
     *
     * @param number                  生成的题目数量
     * @param appAiSysMessageConfig   系统提示词
     * @param appAiUserMessageConfig  用户提示词
     * @param assistantChatMessages   保存上下文的消息列表
     */
    private void invokeAiLoop(int number, String appAiSysMessageConfig, String appAiUserMessageConfig, List<ChatMessage> assistantChatMessages) {
        List<ChatMessage> chatMessages = new ArrayList<>(assistantChatMessages);
        chatMessages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), appAiSysMessageConfig));
        chatMessages.add(new ChatMessage(ChatMessageRole.USER.value(), String.format(appAiUserMessageConfig, Math.min(number, 10))));

        try {
            String s = aiManager.doStableRequest(chatMessages,Boolean.FALSE);
            int start = s.indexOf("[");
            int end = s.lastIndexOf("]");
            if (start != -1 && end != -1) {
                // 提取 JSON 格式的内容
                String content = s.substring(start, end + 1);
                // 保存上下文
                assistantChatMessages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), content));
            } else {
                // 处理未找到 '[' 或 ']' 的情况，记录错误
                System.err.println("无效的响应格式: " + s);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }


}
