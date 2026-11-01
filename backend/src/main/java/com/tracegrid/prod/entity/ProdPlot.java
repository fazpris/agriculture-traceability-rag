package com.tracegrid.prod.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("prod_plot")
public class ProdPlot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private String name;
    private java.math.BigDecimal area;
    private String location;
    private String soilType;
    private String remark;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
