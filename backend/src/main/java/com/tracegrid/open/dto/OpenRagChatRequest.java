package com.tracegrid.open.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpenRagChatRequest {

    @NotBlank(message = "溯源码不能为空")
    private String productCode;

    @NotBlank(message = "问题不能为空")
    private String question;

    @NotBlank(message = "会话标识不能为空")
    private String sessionId;
}
