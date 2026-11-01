package com.tracegrid.complaint.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("complaint_feedback")
public class ComplaintFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceCode;
    private String contact;
    private String content;
    /** 0待处理 1处理中 2已处理 */
    private Integer status;
    private String handleResult;
    private Long handleBy;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
