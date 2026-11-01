import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadMessageCount } from '@/api/sys'

export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)
  const timer = ref(null)

  // 获取未读消息数
  const fetchUnreadCount = async () => {
    try {
      const res = await getUnreadMessageCount()
      unreadCount.value = res || 0
    } catch (error) {
      console.error('获取未读消息数失败:', error)
    }
  }

  // 开始轮询 (每30秒)
  const startPolling = () => {
    stopPolling()
    fetchUnreadCount()
    timer.value = setInterval(fetchUnreadCount, 30000)
  }

  // 停止轮询
  const stopPolling = () => {
    if (timer.value) {
      clearInterval(timer.value)
      timer.value = null
    }
  }

  return {
    unreadCount,
    fetchUnreadCount,
    startPolling,
    stopPolling
  }
})
