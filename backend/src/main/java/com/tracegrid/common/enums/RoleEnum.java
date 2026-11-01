package com.tracegrid.common.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum RoleEnum {
    ADMIN(1, "管理员"),
    PRODUCER(2, "生产主体"),
    PROCESSOR(3, "加工企业"),
    DISTRIBUTOR(4, "流通经销");

    private final Integer code;
    private final String label;

    RoleEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static RoleEnum of(Integer code) {
        for (RoleEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
