package com.tracegrid.dist.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dist_logistics_node")
public class DistLogisticsNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long logisticsId;
    private String nodeName;
    private String location;
    private LocalDateTime arriveTime;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
}
