package com.zaki.zakidada.model.dto.userAnswer;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新用户答案请求
 *
* @author <a href="https://github.com/1327722636">zaki</a>
 */
@Data
public class UserAnswerUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 应用 id
     */
    private Long appId;
    /**
     * 用户答案（JSON 数组）
     */
    private List<String> choices;
}