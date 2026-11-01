import request from './request'

// 获取管理后台概览指标
export function getAdminOverview() {
  return request.get('/sys/stats/admin-overview')
}

// 获取批次增长趋势
export function getBatchTrend() {
  return request.get('/sys/stats/batch-trend')
}

// 获取用户角色分布
export function getUserRoleDist() {
  return request.get('/sys/stats/user-role-dist')
}

// 获取详细业务指标
export function getFullMetrics() {
  return request.get('/sys/stats/full-metrics')
}

// 获取活跃度排行
export function getEntityRank() {
  return request.get('/sys/stats/entity-rank')
}

// 获取业务端概览指标（根据角色/组织动态获取）
export function getBusinessOverview() {
  return request.get('/sys/stats/business-overview')
}

// Compatibility wrappers for role-specific dashboards.
// The backend now serves a unified business overview endpoint.
export async function getProdOverview() {
  const metrics = await getBusinessOverview()
  return { metrics }
}

export async function getProcOverview() {
  const metrics = await getBusinessOverview()
  return { metrics }
}

export async function getDistOverview() {
  const metrics = await getBusinessOverview()
  return { metrics }
}

/** 流通经销看板：近7天出入库/销售笔数趋势 + 全量销售渠道占比 */
export function getDistCharts() {
  return request.get('/sys/stats/dist-charts')
}
