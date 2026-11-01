package com.tracegrid.common.result;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一接口返回体
 */
@Data
public class R<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;

    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.code = 200;
        r.msg = "操作成功";
        return r;
    }

    public static <T> R<T> success(String msg) {
        R<T> r = ok();
        r.msg = msg;
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.data = data;
        return r;
    }

    public static <T> R<T> ok(String msg, T data) {
        R<T> r = ok();
        r.msg = msg;
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }

    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
