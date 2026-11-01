import request from './request'

export const login = (data) => request.post('/auth/login', data)
export const logout = () => request.post('/auth/logout')
export const getMe = () => request.get('/auth/me')
export const getMyOrgProfile = () => request.get('/auth/my-org')
export const changeMyPassword = (data) => request.post('/auth/change-password', data)
export const updateMyPhone = (phone) => request.put('/auth/me/phone', { phone })
export const registerApply = (data) => request.post('/sys/register/apply', data)
