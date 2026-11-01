package com.tracegrid.dist.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dist_logistics_record")
public class DistLogisticsRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private String traceCode;
    private String carrier;
    private String waybillNo;
    private String origin;
    private String destination;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
