package com.tracegrid.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("trace_code")
public class TraceCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Long prodBatchId;
    private Long procBatchId;
    private String qrUrl;
    /** 0未启用 1已生成 2已打印 3已入库 4已流通 5已售出 6已作废 8已过期 9已下架 */
    private Integer status;
    private Integer scanCount;
    private LocalDateTime firstScanTime;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
