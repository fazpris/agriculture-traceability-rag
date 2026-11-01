import request from './request'

export const getInoutList = (params) => request.get('/dist/inout-record/list', { params })
export const getInoutBalance = (traceCode) =>
  request.get('/dist/inout-record/balance', { params: { traceCode } })
/** 入库核销：按追溯码查询未核销的加工出库流水 */
export const getProcSourceOptions = (traceCode) =>
  request.get('/dist/inout-record/proc-source-options', { params: { traceCode } })
export const addInout = (data) => request.post('/dist/inout-record', data)

export const getLogisticsList = (params) => request.get('/dist/logistics/list', { params })
export const addLogistics = (data) => request.post('/dist/logistics', data)
export const addLogisticsNode = (id, data) => request.post(`/dist/logistics/${id}/node`, data)
export const getLogisticsNodes = (id) => request.get(`/dist/logistics/${id}/nodes`)

export const getSaleList = (params) => request.get('/dist/sale-record/list', { params })
export const addSale = (data) => request.post('/dist/sale-record', data)
