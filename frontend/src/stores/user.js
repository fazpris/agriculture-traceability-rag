import { defineStore } from 'pinia'
import { getMe } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.userInfo?.role,
    isAdmin: (state) => state.userInfo?.role === 1,
    isProducer: (state) => state.userInfo?.role === 2,
    isProcessor: (state) => state.userInfo?.role === 3,
    isDistributor: (state) => state.userInfo?.role === 4,
    homePath: (state) => {
      const roleMap = { 1: '/admin/dashboard', 2: '/prod/dashboard', 3: '/proc/dashboard', 4: '/dist/dashboard' }
      return roleMap[state.userInfo?.role] || '/public/query'
    }
  },

  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    clearToken() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    },
    async fetchMe() {
      const data = await getMe()
      this.userInfo = data
    },
    logout() {
      this.clearToken()
    }
  }
})
