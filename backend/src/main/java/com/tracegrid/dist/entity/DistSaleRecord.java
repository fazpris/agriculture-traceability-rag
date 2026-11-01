package com.tracegrid.dist.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dist_sale_record")
public class DistSaleRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private String traceCode;
    private String storeName;
    private String address;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime saleDate;
    private BigDecimal salePrice;
    private String purchaseMethod;
    private String antiCounterfeitTip;
    private BigDecimal quantity;
    private String unit;
    private String remark;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
