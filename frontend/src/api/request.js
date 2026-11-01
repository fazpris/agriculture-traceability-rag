import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// ✅ 全脱离方案：直接从其存储读取，不依赖 Pinia，彻底解决白屏循环依赖
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 适配后端 R.java 统一结构
    if (res.code === 200) return res.data
    ElMessage.error(res.msg || '业务处理失败')
    return Promise.reject(new Error(res.msg))
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.msg

    if (status === 401) {
      localStorage.removeItem('token')
      ElMessage.warning('登录已过期，正在跳转登录页')
      // 使用原生跳转打破死锁
      window.location.href = '/auth/login'
    } else if (status === 403) {
      ElMessage.error('权限不足')
      window.location.href = '/403'
    } else {
      ElMessage.error(msg || '系统繁忙，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
