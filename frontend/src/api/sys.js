import request from './request'

export const getTraceList = (params) => request.get('/trace/list', { params })
export const generateTrace = (data) => request.post('/trace/generate', data)
export const voidTrace = (id) => request.post(`/trace/${id}/void`)
export const batchVoidTrace = (ids) => request.post('/trace/batch-void', ids)
export const getTraceDetail = (id) => request.get(`/trace/${id}`)
export const getTraceGenerationRules = () => request.get('/trace/generation-rules')
export const saveTraceGenerationRules = (data) => request.put('/trace/generation-rules', data)

export const queryByCode = (code) => request.get('/open/query', { params: { code } })

/** 溯源详情页 RAG 问答（经后端转发至 rag/app_api） */
export const ragChat = (data) =>
  request.post('/open/rag/chat', data, { timeout: 120000 })
export const getRegisterStatus = (username, phone) => request.get('/sys/register/query-status', { params: { username, phone } })
export const submitComplaint = (data) => request.post('/open/complaint', data)

export const getRegisterList = (params) => request.get('/sys/register/list', { params })
export const approveRegister = (id) => request.post(`/sys/register/${id}/approve`)
export const rejectRegister = (id, reason) => request.post(`/sys/register/${id}/reject`, null, { params: { reason } })
export const batchApproveRegister = (ids) => request.post('/sys/register/batch-approve', ids)
export const batchRejectRegister = (data) => request.post('/sys/register/batch-reject', data)
export const submitCertReapply = (data) => request.post('/sys/register/cert-reapply', data)

export const getUserList = (params) => request.get('/sys/user/list', { params })
export const toggleUserStatus = (id) => request.post(`/sys/user/${id}/toggle-status`)
export const deleteUser = (id) => request.delete(`/sys/user/${id}`)
export const saveUser = (data) => request.post('/sys/user/save', data)
export const resetUserPassword = (id, password) => request.post(`/sys/user/${id}/reset-password`, { password })
export const getServerIp = () => request.get('/sys/user/server-ip')

export const getMyMessages = (params) => request.get('/sys/message/list', { params })
export const markMessageRead = (id) => request.post(`/sys/message/${id}/read`)
export const getUnreadMessageCount = () => request.get('/sys/message/unread-count')

export const getLogList = (params) => request.get('/sys/log/list', { params })

/** 加工/流通：当前主体核销开关 */
export const getMyInventoryLinkFlags = () => request.get('/sys/org/my-inventory-link-flags')
/** 管理员：主体列表（含核销开关字段） */
export const getOrgList = (params) => request.get('/sys/org/list', { params })
export const updateOrgInventoryLinkFlags = (id, data) => request.put(`/sys/org/${id}/inventory-link-flags`, data)
