package com.tracegrid.proc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("proc_process_record")
public class ProcProcessRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private Long procBatchId;
    private Long rawInboundId;
    private String actionName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String operator;
    private String remark;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
}
