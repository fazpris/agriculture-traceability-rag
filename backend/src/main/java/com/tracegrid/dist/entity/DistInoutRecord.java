package com.tracegrid.dist.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("dist_inout_record")
public class DistInoutRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private String traceCode;
    private Long procBatchId;
    /** 关联的加工成品出库流水 proc_inventory_record.id（一对一核销） */
    private Long sourceProcInventoryId;
    /** 1入库 2出库 */
    private Integer inoutType;
    private BigDecimal quantity;
    private LocalDate operateDate;
    private String remark;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
