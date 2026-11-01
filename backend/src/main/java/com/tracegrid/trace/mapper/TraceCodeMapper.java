package com.tracegrid.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tracegrid.trace.entity.TraceCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TraceCodeMapper extends BaseMapper<TraceCode> {

    /** 原子递增扫码次数，避免高并发下 read-modify-write 行锁竞争 */
    @Update("""
            UPDATE trace_code
            SET scan_count = IFNULL(scan_count, 0) + 1,
                first_scan_time = IFNULL(first_scan_time, NOW()),
                updated_at = NOW()
            WHERE id = #{id} AND deleted = 0
            """)
    int incrementScanCount(@Param("id") Long id);
}
