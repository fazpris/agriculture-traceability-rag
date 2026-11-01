package com.tracegrid.integration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.dist.entity.DistInoutRecord;
import com.tracegrid.proc.entity.ProcInventoryRecord;
import com.tracegrid.proc.mapper.ProcInventoryRecordMapper;
import com.tracegrid.sys.entity.SysOrg;
import com.tracegrid.sys.mapper.SysOrgMapper;
import com.tracegrid.trace.entity.TraceCode;
import com.tracegrid.trace.mapper.TraceCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 加工成品出库与流通入库一对一核销（按主体 sys_org 开关配置）。
 */
@Service
@RequiredArgsConstructor
public class ProcDistStockLinkService {

    private final SysOrgMapper orgMapper;
    private final ProcInventoryRecordMapper procInventoryMapper;
    private final TraceCodeMapper traceCodeMapper;

    public boolean isProcOutboundLinkEnabled(Long orgId) {
        if (orgId == null) {
            return false;
        }
        SysOrg org = orgMapper.selectById(orgId);
        return org != null && org.getType() != null && org.getType() == 2
                && org.getProcDistLinkOutbound() != null && org.getProcDistLinkOutbound() == 1;
    }

    public boolean isDistInboundLinkRequired(Long orgId) {
        if (orgId == null) {
            return false;
        }
        SysOrg org = orgMapper.selectById(orgId);
        return org != null && org.getType() != null && org.getType() == 3
                && org.getProcDistLinkInbound() != null && org.getProcDistLinkInbound() == 1;
    }

    /**
     * 加工「出库」流水插入前：若主体开启开关，则标记待核销。
     */
    public void validateInboundBeforeInsert(DistInoutRecord inbound, Long distOrgId) {
        if (inbound.getInoutType() == null || inbound.getInoutType() != 1) {
            return;
        }
        if (isDistInboundLinkRequired(distOrgId) && inbound.getSourceProcInventoryId() == null) {
            throw BusinessException.of("当前主体已开启「入库须关联加工出库」：请选择或填写加工出库流水号（sourceProcInventoryId）。");
        }
    }

    public void applyOutboundPendingFlag(ProcInventoryRecord inv, Long procOrgId) {
        if (inv.getOperateType() == null || inv.getOperateType() != 2) {
            return;
        }
        if (!isProcOutboundLinkEnabled(procOrgId)) {
            inv.setDistLinkPending(0);
            return;
        }
        inv.setDistLinkPending(1);
        inv.setLinkedDistInoutId(null);
    }

    /**
     * 流通入库保存后：若携带 sourceProcInventoryId，则核销对应加工出库流水（一对一）。
     */
    public void settleInboundAfterInsert(DistInoutRecord inbound, Long distOrgId) {
        if (inbound.getInoutType() == null || inbound.getInoutType() != 1) {
            return;
        }
        if (inbound.getSourceProcInventoryId() == null) {
            return;
        }
        String trace = inbound.getTraceCode() == null ? "" : inbound.getTraceCode().trim();
        if (trace.isEmpty()) {
            throw BusinessException.of("核销入库须填写追溯码");
        }
        TraceCode tc = traceCodeMapper.selectOne(
                new LambdaQueryWrapper<TraceCode>().apply("TRIM(code) = {0}", trace));
        if (tc == null || tc.getProcBatchId() == null) {
            throw BusinessException.of("追溯码不存在或未绑定加工成品批次，无法核销");
        }
        ProcInventoryRecord src = procInventoryMapper.selectById(inbound.getSourceProcInventoryId());
        if (src == null) {
            throw BusinessException.of("加工出库流水不存在");
        }
        if (src.getOperateType() == null || src.getOperateType() != 2) {
            throw BusinessException.of("关联记录必须是加工成品「出库」流水");
        }
        if (src.getLinkedDistInoutId() != null) {
            throw BusinessException.of("该加工出库流水已核销，不可重复使用");
        }
        if (src.getProductBatchId() == null || !src.getProductBatchId().equals(tc.getProcBatchId())) {
            throw BusinessException.of("加工出库流水与追溯码绑定的成品批次不一致，无法核销");
        }
        BigDecimal iq = inbound.getQuantity() == null ? BigDecimal.ZERO : inbound.getQuantity();
        BigDecimal sq = src.getQuantity() == null ? BigDecimal.ZERO : src.getQuantity();
        if (iq.compareTo(sq) != 0) {
            throw BusinessException.of(String.format(
                    "一对一核销要求数量完全一致：流通入库 %s，加工出库流水 %s。",
                    strip(iq), strip(sq)));
        }
        src.setLinkedDistInoutId(inbound.getId());
        src.setDistLinkPending(0);
        procInventoryMapper.updateById(src);
    }

    /**
     * 供流通端下拉：某追溯码下尚未核销的加工出库流水（待核销优先）。
     */
    public List<ProcInventoryRecord> listUnsettledProcOutboundsForTrace(String traceCode) {
        String code = traceCode == null ? "" : traceCode.trim();
        if (code.isEmpty()) {
            return List.of();
        }
        TraceCode tc = traceCodeMapper.selectOne(
                new LambdaQueryWrapper<TraceCode>().apply("TRIM(code) = {0}", code));
        if (tc == null || tc.getProcBatchId() == null) {
            return List.of();
        }
        return procInventoryMapper.selectList(
                new LambdaQueryWrapper<ProcInventoryRecord>()
                        .eq(ProcInventoryRecord::getProductBatchId, tc.getProcBatchId())
                        .eq(ProcInventoryRecord::getOperateType, 2)
                        .isNull(ProcInventoryRecord::getLinkedDistInoutId)
                        .orderByDesc(ProcInventoryRecord::getDistLinkPending)
                        .orderByDesc(ProcInventoryRecord::getCreatedAt));
    }

    private static String strip(BigDecimal n) {
        return n == null ? "0" : n.stripTrailingZeros().toPlainString();
    }
}
