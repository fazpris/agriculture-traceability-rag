package com.tracegrid.prod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("prod_batch")
public class ProdBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private Long orgId;
    private Long plotId;
    private String productName;
    private LocalDate harvestDate;
    private BigDecimal quantity;
    private String unit;
    private String remark;
    /** 0待审核 1审核通过 2审核驳回（若业务上需「已入加工库」请另扩状态值，勿与驳回混用） */
    private Integer status;
    /** 审核驳回原因 */
    private String rejectReason;
    private Long auditBy;
    private LocalDateTime auditAt;
    @TableField(exist = false)
    private String auditorName;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
