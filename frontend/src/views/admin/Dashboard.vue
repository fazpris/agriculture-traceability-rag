<template>
  <div class="page-container dashboard-admin">
    <div class="page-header">
      <h2 class="page-header__title">系统概览</h2>
      <div class="page-header__actions">
        <el-button type="primary" plain :icon="Refresh" circle @click="loadData" />
      </div>
    </div>

    <!-- 运营待办类指标 -->
    <el-row :gutter="24" class="metrics-row">
      <el-col :span="6" v-for="m in metrics" :key="'o-' + m.label">
        <div class="luxury-metric-card">
          <div class="card-glass"></div>
          <div class="m-content">
            <div class="m-header">
              <span class="m-label">{{ m.label }}</span>
              <div class="m-icon" :style="{ background: m.color + '15', color: m.color }">
                <el-icon :size="20"><component :is="m.icon" /></el-icon>
              </div>
            </div>
            <div class="m-value">{{ m.value }}</div>
            <div class="m-footer">
              <span class="trend" :class="{ up: m.up }">
                <el-icon><component :is="m.up ? 'CaretTop' : 'CaretBottom'" /></el-icon>
                {{ m.trend }}
              </span>
              <span class="period">较上月同期</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 业务统计（原「统计分析」页核心指标） -->
    <el-row :gutter="24" class="metrics-row metrics-row--secondary">
      <el-col :span="6" v-for="m in fullMetrics" :key="'f-' + m.label">
        <div class="luxury-metric-card">
          <div class="card-glass"></div>
          <div class="m-content">
            <div class="m-header">
              <span class="m-label">{{ m.label }}</span>
              <div class="m-icon" :style="{ background: m.color + '15', color: m.color }">
                <el-icon :size="20"><component :is="m.icon" /></el-icon>
              </div>
            </div>
            <div class="m-value">{{ m.value }}</div>
            <div class="m-footer">
              <span class="trend" :class="{ up: m.up }">
                <el-icon><component :is="m.up ? 'CaretTop' : 'CaretBottom'" /></el-icon>
                {{ m.trend }}
              </span>
              <span class="period">较上月同期</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 待办提醒区 -->
    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="24">
        <div class="todo-scroller">
          <div v-for="t in todos" :key="t.title" class="todo-pill" @click="router.push(t.path)">
            <el-icon :color="t.color"><component :is="t.icon" /></el-icon>
            <span class="t-name">{{ t.title }}</span>
            <span class="t-count">{{ t.count }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 中层数据图表 -->
    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="16">
        <div class="luxury-card chart-card">
          <div class="card-header">
            <div class="title-group">
              <span class="indicator"></span>
              <h3>近30天全链路日新增</h3>
            </div>
            <div class="actions">
              <el-radio-group v-model="trendPeriod" size="small">
                <el-radio-button label="7d">WEEK</el-radio-button>
                <el-radio-button label="30d">MONTH</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <div class="card-body">
            <div ref="lineChartEl" style="height: 320px;"></div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="luxury-card chart-card">
          <div class="card-header">
            <div class="title-group">
              <span class="indicator accent"></span>
              <h3>用户生态分布</h3>
            </div>
          </div>
          <div class="card-body">
            <div ref="pieChartEl" style="height: 320px;"></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 近期入驻 + 主体活跃度 -->
    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :xs="24" :lg="14">
        <div class="luxury-card table-card">
          <div class="card-header">
            <div class="title-group">
              <span class="indicator warning"></span>
              <h3>近期入驻审核单</h3>
            </div>
            <el-button link type="primary" class="fancy-link" @click="router.push('/admin/register-audit')">
              VIEW ALL DISPATCHES <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div class="card-body" style="padding: 0;">
            <el-table :data="pendingList" class="luxury-table">
              <el-table-column prop="orgName" label="主体档案名称" min-width="200">
                <template #default="{ row }">
                  <div class="org-cell">
                    <el-avatar :size="32" shape="square" class="org-avatar">{{ row.orgName[0] }}</el-avatar>
                    <span>{{ row.orgName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="role" label="属性" width="120">
                <template #default="{ row }">
                  <span class="role-badge" :class="'role-' + row.role">{{ roleMap[row.role] }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="提交时间" width="180" />
              <el-table-column label="审核优先级" width="120">
                <template #default>
                  <div class="priority-label"><span class="dot"></span> NORMAL</div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default>
                  <el-button type="primary" size="small" circle :icon="Check" @click="router.push('/admin/register-audit')" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="luxury-card table-card">
          <div class="card-header">
            <div class="title-group">
              <span class="indicator"></span>
              <h3>主体业务活跃度 TOP5</h3>
            </div>
          </div>
          <div class="card-body" style="padding: 0;">
            <el-table :data="rankData" class="luxury-table">
              <el-table-column type="index" label="排名" width="64" align="center" />
              <el-table-column prop="name" label="主体" min-width="120" show-overflow-tooltip />
              <el-table-column prop="type" label="类型" width="100">
                <template #default="{ row }">
                  <span class="type-badge">{{ row.type }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="batchCount" label="业务量" width="88" align="center" />
              <el-table-column prop="activeRate" label="活跃" min-width="100">
                <template #default="{ row }">
                  <div class="activity-wrap">
                    <el-progress
                      :percentage="row.activeRate"
                      :color="progressColor(row.activeRate)"
                      :stroke-width="6"
                      :show-text="false"
                    />
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getRegisterList } from '@/api/sys'
import { getBatchAuditList } from '@/api/prod'
import { getRawInboundAuditList } from '@/api/proc'
import { getAdminOverview, getFullMetrics, getBatchTrend, getUserRoleDist, getEntityRank } from '@/api/stats'
import { Refresh, CaretTop, CaretBottom, ArrowRight, Check } from '@element-plus/icons-vue'

const router = useRouter()
const lineChartEl = ref(null)
const pieChartEl = ref(null)
let lineChartInst = null
let pieChartInst = null
const pendingList = ref([])
const rankData = ref([])
const metrics = ref([])
const fullMetrics = ref([])
const todos = ref([])
const trendPeriod = ref('30d')
const rawTrend = ref(null)
const rawPie = ref(null)

const roleMap = { 1: '管理员', 2: '农事基地', 3: '加工工厂', 4: '贸易流通' }

const progressColor = (rate) => {
  if (rate > 80) return '#1A7A4A'
  if (rate > 40) return '#C9A227'
  return '#94A3B8'
}

function sliceTrendByPeriod(lData) {
  if (!lData || !lData.xData) return lData
  const n = trendPeriod.value === '7d' ? 7 : 30
  const len = lData.xData.length
  const start = Math.max(0, len - n)
  const slice = (arr) => (Array.isArray(arr) ? arr.slice(start) : arr)
  const next = {
    ...lData,
    xData: slice(lData.xData),
    yData: lData.yData != null ? slice(lData.yData) : lData.yData
  }
  if (lData.series && lData.series.length) {
    next.series = lData.series.map((s) => ({
      ...s,
      yData: slice(s.yData || [])
    }))
  }
  return next
}

const initCharts = (lData, pData) => {
  const sliced = sliceTrendByPeriod(lData)
  if (lineChartEl.value && sliced && sliced.xData) {
    if (!lineChartInst) lineChartInst = echarts.init(lineChartEl.value)
    else lineChartInst.clear()
    const colors = ['#1A7A4A', '#3B82F6', '#C9A227', '#10B981']
    const series = (sliced.series && sliced.series.length
      ? sliced.series.map((s, i) => ({
          name: s.name,
          type: 'line',
          data: s.yData || [],
          smooth: true,
          showSymbol: false,
          lineStyle: { width: 2, color: colors[i % colors.length] }
        }))
      : [{
          name: '日新增合计',
          type: 'line',
          data: sliced.yData || [],
          smooth: true,
          showSymbol: false,
          lineStyle: { width: 4, color: '#1A7A4A' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(26, 122, 74, 0.12)' },
              { offset: 1, color: 'rgba(26, 122, 74, 0)' }
            ])
          }
        }])
    lineChartInst.setOption({
      grid: { left: '3%', right: '4%', top: '12%', bottom: sliced.series?.length ? '16%' : '3%', containLabel: true },
      legend: sliced.series?.length ? { bottom: 0, textStyle: { color: '#64748B', fontSize: 11 } } : undefined,
      xAxis: {
        type: 'category',
        data: sliced.xData,
        axisLine: { lineStyle: { color: '#F1F3F5' } },
        axisLabel: { color: '#94A3B8', fontSize: 11, margin: 15 },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#F8FAFB', type: 'dashed' } },
        axisLabel: { color: '#94A3B8' }
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 12,
        padding: 12,
        textStyle: { color: '#0F172A', fontSize: 12 },
        borderWidth: 0,
        boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)'
      },
      series
    })
  }

  if (pieChartEl.value && pData) {
    if (!pieChartInst) pieChartInst = echarts.init(pieChartEl.value)
    else pieChartInst.clear()
    pieChartInst.setOption({
      color: ['#0A2A1A', '#1A7A4A', '#C9A227', '#E5E7EB'],
      tooltip: { trigger: 'item', backgroundColor: '#fff', borderRadius: 8 },
      legend: { bottom: '0%', left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: '#64748B', fontSize: 11 } },
      series: [{
        type: 'pie',
        radius: ['55%', '85%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 4 },
        label: { show: false },
        data: pData
      }]
    })
  }
}

const loadData = async () => {
  try {
    const [mData, fmData, lData, pData, res, rData, batchAuditRes, inboundAuditRes] = await Promise.all([
      getAdminOverview(),
      getFullMetrics(),
      getBatchTrend(),
      getUserRoleDist(),
      getRegisterList({ page: 1, size: 5, status: 0 }),
      getEntityRank(),
      getBatchAuditList({ page: 1, size: 1, status: 0 }),
      getRawInboundAuditList({ page: 1, size: 1, status: 1 })
    ])
    metrics.value = mData || []
    fullMetrics.value = fmData || []
    rawTrend.value = lData
    rawPie.value = pData
    pendingList.value = res.records || []
    rankData.value = rData || []

    const complaintMetric = (mData || []).find((m) => m.label === '待处理投诉')
    const complaintCount = complaintMetric != null ? Number(complaintMetric.value) : 0
    todos.value = [
      { title: '注册审核', count: res.total || 0, icon: 'DocumentChecked', color: '#1A7A4A', path: '/admin/register-audit' },
      { title: '原料审核', count: inboundAuditRes?.total || 0, icon: 'CopyDocument', color: '#3B82F6', path: '/admin/inbound-audit' },
      { title: '生产批次审核', count: batchAuditRes?.total || 0, icon: 'CircleCheck', color: '#059669', path: '/admin/batch-audit' },
      { title: '待处理投诉', count: Number.isFinite(complaintCount) ? complaintCount : 0, icon: 'ChatLineSquare', color: '#C9A227', path: '/admin/complaint' }
    ]

    await nextTick()
    initCharts(lData, pData)
  } catch (err) {
    console.error(err)
  }
}

watch(trendPeriod, () => {
  if (rawTrend.value && rawPie.value) initCharts(rawTrend.value, rawPie.value)
})

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.dashboard-admin {
  background: var(--color-bg-base);
}

.metrics-row--secondary {
  margin-top: 16px;
}

.luxury-metric-card {
  position: relative;
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  border: 1px solid #F1F3F5;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 40px rgba(0,0,0,0.06);
    .card-glass { transform: scale(1.1); }
  }

  .card-glass {
    position: absolute;
    top: -20px; right: -20px; width: 100px; height: 100px;
    background: radial-gradient(circle, rgba(102, 102, 102, 0.03) 0%, transparent 70%);
    transition: inherit;
  }

  .m-header {
    @include flex(row, space-between, center);
    margin-bottom: 20px;
    .m-label { font-size: 13px; font-weight: 600; color: #64748B; letter-spacing: 0.2px; }
  }

  .m-value {
    font-family: 'Outfit', sans-serif;
    font-size: 32px;
    font-weight: 700;
    color: var(--color-primary);
    line-height: 1;
    margin-bottom: 12px;
  }

  .m-footer {
    @include flex(row, flex-start, center);
    gap: 8px;
    font-size: 11px;
    .trend {
      font-weight: 700; color: #94A3B8;
      @include flex(row, flex-start, center); gap: 2px;
      &.up { color: var(--color-success); }
    }
    .period { color: #CBD5E1; font-weight: 600; }
  }

  .m-icon {
    width: 40px; height: 40px; border-radius: 12px;
    @include flex(row, center, center);
  }
}

.luxury-card {
  background: #fff;
  border-radius: 24px;
  border: 1px solid #F1F3F5;
  box-shadow: 0 4px 20px rgba(0,0,0,0.02);
  overflow: hidden;

  .card-header {
    padding: 24px;
    @include flex(row, space-between, center);
    border-bottom: 1px solid #F8FAFB;
    
    .title-group {
      @include flex(row, flex-start, center); gap: 12px;
      .indicator { width: 4px; height: 16px; background: var(--color-primary); border-radius: 2px; }
      .indicator.accent { background: var(--color-accent); }
      .indicator.warning { background: var(--color-warning); }
      h3 { font-size: 16px; font-weight: 700; color: var(--color-primary); margin: 0; }
    }
  }

  .card-body { padding: 24px; }
}

.role-badge {
  padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 600;
  &.role-1 { background: #F1F5F9; color: #475569; }
  &.role-2 { background: #F0FDF4; color: #166534; }
  &.role-3 { background: #EFF6FF; color: #1D4ED8; }
  &.role-4 { background: #FFF7ED; color: #9A3412; }
}

.type-badge {
  background: #F8FAFC;
  color: #64748B;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.activity-wrap {
  padding-right: 8px;
}

.org-cell {
  @include flex(row, flex-start, center); gap: 12px;
  span { font-weight: 500; font-size: 14px; }
  .org-avatar { background: var(--color-primary); font-family: var(--font-mono); font-weight: 700; }
}

.fancy-link {
  font-size: 11px; font-weight: 800; letter-spacing: 1px;
}

.priority-label {
  font-size: 11px; font-weight: 700; color: #94A3B8;
  @include flex(row, flex-start, center); gap: 6px;
  .dot { width: 6px; height: 6px; border-radius: 50%; background: #E2E8F0; }
}

.luxury-table {
  :deep(th.el-table__cell) { background: #F8FAFB !important; color: #64748B; padding: 16px 0; }
  :deep(td.el-table__cell) { padding: 12px 0; border-bottom: 1px solid #F1F5F9; }
}

.todo-scroller {
  display: flex; gap: 16px; overflow-x: auto; padding-bottom: 8px;
  &::-webkit-scrollbar { height: 4px; }
  &::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 4px; }
  
  .todo-pill {
    flex-shrink: 0; background: #fff; border-radius: 50px; padding: 10px 20px;
    border: 1px solid #F1F3F5; cursor: pointer; transition: all 0.3s ease;
    @include flex(row, flex-start, center); gap: 10px;
    
    &:hover { border-color: var(--color-primary); background: #F0FDF4; }
    
    .t-name { font-size: 13px; font-weight: 600; color: #475569; }
    .t-count {
      min-width: 20px; height: 20px; background: #FEE2E2; color: #991B1B;
      border-radius: 10px; font-size: 11px; @include flex(row, center, center);
      font-weight: 800; padding: 0 6px;
    }
  }
}
</style>
