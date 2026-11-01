package com.tracegrid.proc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("proc_product_batch")
public class ProcProductBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private Long orgId;
    private Long rawInboundId;
    private String productName;
    /** 产品规格，如 300mL/瓶、5kg/袋 */
    private String productSpec;
    /** 配料或成分说明 */
    private String ingredients;
    /** 产品产地 */
    private String productOrigin;
    /** 保质期限，如 12个月、180天 */
    private String shelfLife;
    /** 储存条件 */
    private String storageCondition;
    private LocalDate finishDate;
    private BigDecimal quantity;
    private String unit;
    /** 0待质检 1合格 2不合格 */
    private Integer qualityStatus;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
