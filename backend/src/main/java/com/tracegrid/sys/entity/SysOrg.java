package com.tracegrid.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_org")
public class SysOrg {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer type;
    private String address;
    private String contact;
    private String phone;
    private String licenseNo;
    /** 加工主体：成品出库是否产生「待流通入库核销」记录 */
    private Integer procDistLinkOutbound;
    /** 流通主体：入库是否必须关联加工成品出库流水（一对一核销） */
    private Integer procDistLinkInbound;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
