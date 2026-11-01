package com.tracegrid.prod.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("prod_farming_record")
public class ProdFarmingRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private Long plotId;
    /** 播种/施肥/灌溉/用药/除草/采收 */
    private String actionType;
    private LocalDate actionDate;
    private String detail;
    private String operator;
    /** 农事活动相关图片，JSON格式存储 */
    private String images;
    /** 0草稿 1已提交 2已生效 3已作废 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
