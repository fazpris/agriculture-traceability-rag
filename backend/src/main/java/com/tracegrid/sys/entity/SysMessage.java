package com.tracegrid.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_message")
public class SysMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    /** 0消息 1审核 2投诉 */
    private Integer type;
    /** 0未读 1已读 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
