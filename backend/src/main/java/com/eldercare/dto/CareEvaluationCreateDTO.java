package com.eldercare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 家属提交评价。orderId 来自路径；elderId/familyUserId 由服务端从订单与 JWT 写入。
 */
public class CareEvaluationCreateDTO {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1")
    @Max(value = 5, message = "评分最高为5")
    private Integer score;

    @Size(max = 500, message = "评价内容不能超过500字")
    private String content;

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
