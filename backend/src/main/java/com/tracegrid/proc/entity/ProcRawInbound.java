package com.tracegrid.proc.entity;

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
@TableName("proc_raw_inbound")
public class ProcRawInbound {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private Long prodBatchId;
    private LocalDate inboundDate;
    private BigDecimal quantity;
    private String inspector;
    private String remark;
    private Integer status;
    /** 驳回原因 */
    private String rejectReason;
    /** 审核人 */
    private Long auditBy;
    /** 审核时间 */
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
