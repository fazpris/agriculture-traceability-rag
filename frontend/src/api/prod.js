import request from './request'

export const getPlotList = (params) => request.get('/prod/plot/list', { params })
export const addPlot = (data) => request.post('/prod/plot', data)
export const updatePlot = (id, data) => request.put(`/prod/plot/${id}`, data)
export const deletePlot = (id) => request.delete(`/prod/plot/${id}`)

export const getFarmingList = (params) => request.get('/prod/farming-record/list', { params })
export const addFarming = (data) => request.post('/prod/farming-record', data)
export const updateFarming = (id, data) => request.put(`/prod/farming-record/${id}`, data)
export const submitFarming = (id) => request.post(`/prod/farming-record/${id}/submit`)
export const deleteFarming = (id) => request.delete(`/prod/farming-record/${id}`)

export const getBatchList = (params) => request.get('/prod/batch/list', { params })
export const addBatch = (data) => request.post('/prod/batch', data)
export const getBatchDetail = (id) => request.get(`/prod/batch/${id}`)
export const deleteBatch = (id) => request.delete(`/prod/batch/${id}`)

// 管理员专用：批次审核
export const getBatchAuditList = (params) => request.get('/prod/batch/audit-list', { params })
export const auditProdBatch = (id, status, reason) => request.post(`/prod/batch/${id}/audit`, null, { params: { status, reason } })
