package com.tracegrid.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_register_apply")
public class SysRegisterApply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private Integer role;
    private String orgName;
    private Integer orgType;
    private String orgAddress;
    private String licenseNo;
    /** 0 入驻申请 1 已入驻主体认证资料变更 */
    private Integer applyKind;
    /** applyKind=1 时，待更新的主体 ID（与当前用户 org 一致） */
    private Long targetOrgId;
    /** 0待审核 1通过 2驳回 */
    private Integer status;
    private String rejectReason;
    private Long auditBy;
    private LocalDateTime auditAt;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
