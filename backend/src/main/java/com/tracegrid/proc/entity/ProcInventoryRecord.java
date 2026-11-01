package com.tracegrid.proc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("proc_inventory_record")
public class ProcInventoryRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private Long productBatchId;
    /** 1入库 2出库 3盘点 */
    private Integer operateType;
    private BigDecimal quantity;
    private LocalDate operateDate;
    private String remark;
    /** 已核销对应的流通入库单 dist_inout_record.id */
    private Long linkedDistInoutId;
    /** 1=待下游流通入库核销（与 sys_org.proc_dist_link_outbound 配合） */
    private Integer distLinkPending;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
}
