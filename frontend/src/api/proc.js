import request from './request'

export const getRawInboundList = (params) => request.get('/proc/raw-inbound/list', { params })
export const getRawInboundAuditList = (params) => request.get('/proc/raw-inbound/audit-list', { params })
export const addRawInbound = (data) => request.post('/proc/raw-inbound', data)
export const submitRawInbound = (id) => request.post(`/proc/raw-inbound/${id}/submit`)
export const auditRawInbound = (id, status, reason) => request.post(`/proc/raw-inbound/${id}/audit`, null, { params: { status, reason } })

export const getProductBatchList = (params) => request.get('/proc/product-batch/list', { params })
export const addProductBatch = (data) => request.post('/proc/product-batch', data)
export const getProductBatchDetail = (id) => request.get(`/proc/product-batch/${id}`)

export const getInventoryRecordList = (params) => request.get('/proc/inventory-record/list', { params })
export const getPendingDistInventoryLinks = (params) =>
  request.get('/proc/inventory-record/pending-dist-links', { params })
export const addInventoryRecord = (data) => request.post('/proc/inventory-record', data)

export const getQualityList = (params) => request.get('/proc/quality-inspection/list', { params })
export const addQuality = (data) => request.post('/proc/quality-inspection', data)
export const updateQuality = (id, data) => request.put(`/proc/quality-inspection/${id}`, data)
