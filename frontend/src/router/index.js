import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    // ========== 公开查询 ==========
    {
      path: '/',
      redirect: '/public/query'
    },
    {
      path: '/public',
      component: () => import('@/layouts/PublicLayout.vue'),
      meta: { public: true },
      children: [
        { path: 'query', name: 'PublicQuery', component: () => import('@/views/public/QueryPage.vue') },
        { path: 'result', name: 'PublicResult', component: () => import('@/views/public/ResultPage.vue') },
      ]
    },

    // ========== 认证 ==========
    {
      path: '/auth',
      component: () => import('@/layouts/PublicLayout.vue'),
      meta: { public: true },
      children: [
        { path: 'login', name: 'Login', component: () => import('@/views/auth/LoginPage.vue') },
        { path: 'register', name: 'Register', component: () => import('@/views/auth/RegisterPage.vue') },
      ]
    },

    // ========== 生产端 ==========
    {
      path: '/prod',
      component: () => import('@/layouts/BusinessLayout.vue'),
      meta: { role: [2] },
      children: [
        { path: 'dashboard', name: 'ProdDashboard', component: () => import('@/views/prod/Dashboard.vue'), meta: { title: '工作台' } },
        { path: 'plot', name: 'ProdPlot', component: () => import('@/views/prod/PlotPage.vue'), meta: { title: '地块管理' } },
        { path: 'farming-record', name: 'ProdFarming', component: () => import('@/views/prod/FarmingRecordPage.vue'), meta: { title: '农事记录' } },
        { path: 'batch', name: 'ProdBatch', component: () => import('@/views/prod/BatchPage.vue'), meta: { title: '生产批次' } },
        { path: 'trace-code', name: 'ProdTraceCode', component: () => import('@/views/business/BusinessTraceCodePage.vue'), meta: { title: '溯源码管理' } },
        { path: 'org-profile', name: 'ProdOrgProfile', component: () => import('@/views/business/OrgProfilePage.vue'), meta: { title: '主体信息管理' } },
      ]
    },

    // ========== 加工端 ==========
    {
      path: '/proc',
      component: () => import('@/layouts/BusinessLayout.vue'),
      meta: { role: [3] },
      children: [
        { path: 'dashboard', name: 'ProcDashboard', component: () => import('@/views/proc/Dashboard.vue'), meta: { title: '工作台' } },
        { path: 'raw-inbound', name: 'ProcRawInbound', component: () => import('@/views/proc/RawInboundPage.vue'), meta: { title: '原料入库' } },
        { path: 'quality', name: 'ProcQuality', component: () => import('@/views/proc/QualityPage.vue'), meta: { title: '质检管理' } },
        { path: 'product-batch', name: 'ProcProductBatch', component: () => import('@/views/proc/ProductBatchPage.vue'), meta: { title: '成品批次与库存' } },
        { path: 'inventory', redirect: { name: 'ProcProductBatch', query: { tab: 'stock' } } },
        { path: 'trace-code', name: 'ProcTraceCode', component: () => import('@/views/business/BusinessTraceCodePage.vue'), meta: { title: '溯源码管理' } },
        { path: 'org-profile', name: 'ProcOrgProfile', component: () => import('@/views/business/OrgProfilePage.vue'), meta: { title: '主体信息管理' } },
      ]
    },

    // ========== 流通端 ==========
    {
      path: '/dist',
      component: () => import('@/layouts/BusinessLayout.vue'),
      meta: { role: [4] },
      children: [
        { path: 'dashboard', name: 'DistDashboard', component: () => import('@/views/dist/Dashboard.vue'), meta: { title: '工作台' } },
        { path: 'inout-record', name: 'DistInout', component: () => import('@/views/dist/InoutPage.vue'), meta: { title: '出入库记录' } },
        { path: 'logistics', name: 'DistLogistics', component: () => import('@/views/dist/LogisticsPage.vue'), meta: { title: '物流跟踪' } },
        { path: 'sale-record', name: 'DistSale', component: () => import('@/views/dist/SalePage.vue'), meta: { title: '终端销售' } },
        { path: 'org-profile', name: 'DistOrgProfile', component: () => import('@/views/business/OrgProfilePage.vue'), meta: { title: '主体信息管理' } },
      ]
    },

    // ========== 管理后台 ==========
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { role: [1] },
      children: [
        { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '控制台概览' } },
        { path: 'user', name: 'AdminUser', component: () => import('@/views/admin/UserPage.vue'), meta: { title: '用户管理' } },
        { path: 'register-audit', name: 'RegisterAudit', component: () => import('@/views/admin/RegisterAuditPage.vue'), meta: { title: '注册审核' } },
        { path: 'batch-audit', name: 'BatchAudit', component: () => import('@/views/admin/BatchAuditPage.vue'), meta: { title: '批次审核' } },
        { path: 'inbound-audit', name: 'InboundAudit', component: () => import('@/views/admin/InboundAuditPage.vue'), meta: { title: '入库审核' } },
        { path: 'trace-code', name: 'TraceCode', component: () => import('@/views/admin/TraceCodePage.vue'), meta: { title: '追溯码管理' } },
        { path: 'complaint', name: 'Complaint', component: () => import('@/views/admin/ComplaintPage.vue'), meta: { title: '投诉反馈' } },
        { path: 'stats', redirect: '/admin/dashboard' },
        { path: 'org-inventory-link', name: 'OrgInventoryLink', component: () => import('@/views/admin/OrgInventoryLinkPage.vue'), meta: { title: '库存核销配置' } },
        { path: 'message', name: 'Message', component: () => import('@/views/admin/MessagePage.vue'), meta: { title: '消息中心' } },
      ]
    },

    // 403
    { path: '/403', component: () => import('@/views/auth/ForbiddenPage.vue'), meta: { public: true } },
    // 404
    { path: '/:pathMatch(.*)*', redirect: '/public/query' },
  ]
})

// 全局路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.public) return next()
  if (!userStore.token) return next('/auth/login')
  
  if (!userStore.userInfo) {
    try { 
      await userStore.fetchMe() 
    } catch { 
      userStore.clearToken()
      return next('/auth/login') 
    }
  }
  next()
})

export default router
