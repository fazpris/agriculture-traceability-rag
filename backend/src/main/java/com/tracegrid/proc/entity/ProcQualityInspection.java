package com.tracegrid.proc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("proc_quality_inspection")
public class ProcQualityInspection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private Long procBatchId;
    private LocalDate inspectDate;
    private String institution;
    /** 1合格 2不合格 */
    private Integer result;
    private String conclusion;
    private String reportUrl;
    /** 质检报告相关的图片，JSON格式存储 */
    private String images;
    /** 0待录入 1待审核 2合格 3不合格 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
