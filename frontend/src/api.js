import { store, clearSession, patchUser } from './store'

const BASE = import.meta.env.VITE_API_BASE || '/api'

function buildQuery(params) {
  if (!params) return ''
  const usp = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') usp.append(k, v)
  })
  const qs = usp.toString()
  return qs ? `?${qs}` : ''
}

export function assetUrl(path) {
  if (!path) return ''
  if (/^(https?:)?\/\//.test(path) || path.startsWith('data:')) return path
  return BASE + path
}

export async function request(path, options = {}) {
  const { method = 'GET', body, params, auth = true, formData = null } = options
  const headers = {}
  if (auth && store.token) {
    headers.Authorization = `Bearer ${store.token}`
  }
  let payload
  if (formData) {
    payload = formData
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }

  let res
  try {
    res = await fetch(BASE + path + buildQuery(params), { method, headers, body: payload })
  } catch (e) {
    throw new Error('网络异常，无法连接服务器')
  }

  if (res.status === 401) {
    clearSession()
    const loginPath = `${import.meta.env.BASE_URL}login`
    if (!location.pathname.endsWith('/login')) {
      location.replace(loginPath)
    }
    throw new Error('登录已过期，请重新登录')
  }

  let json = null
  try {
    json = await res.json()
  } catch {
    throw new Error(`服务响应异常（HTTP ${res.status}）`)
  }

  if (res.status === 403) {
    throw new Error(json.message || '无权限访问')
  }
  if (!json || json.code !== 200) {
    throw new Error((json && json.message) || '请求失败')
  }
  return json.data
}

export const api = {
  // 鉴权
  login: (data) => request('/auth/login', { method: 'POST', body: data, auth: false }),
  register: (data) => request('/auth/register', { method: 'POST', body: data, auth: false }),
  // 个人
  profile: () => request('/user/profile'),
  updateProfile: (data) => request('/user/profile', { method: 'PUT', body: data }),
  updatePassword: (data) => request('/user/password', { method: 'PUT', body: data }),
  uploadAvatar: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return request('/user/avatar', { method: 'POST', formData: fd })
  },
  refreshLocalAvatar: (url) => patchUser({ avatar: url }),
  // 楼栋 / 房间 / 座位
  buildings: () => request('/buildings'),
  createBuilding: (data) => request('/buildings', { method: 'POST', body: data }),
  updateBuilding: (id, data) => request(`/buildings/${id}`, { method: 'PUT', body: data }),
  deleteBuilding: (id) => request(`/buildings/${id}`, { method: 'DELETE' }),
  rooms: (buildingId) => request('/rooms', { params: { buildingId } }),
  createRoom: (data) => request('/rooms', { method: 'POST', body: data }),
  updateRoom: (id, data) => request(`/rooms/${id}`, { method: 'PUT', body: data }),
  deleteRoom: (id) => request(`/rooms/${id}`, { method: 'DELETE' }),
  seats: (roomId) => request('/seats', { params: { roomId } }),
  createSeat: (data) => request('/seats', { method: 'POST', body: data }),
  updateSeat: (id, data) => request(`/seats/${id}`, { method: 'PUT', body: data }),
  deleteSeat: (id) => request(`/seats/${id}`, { method: 'DELETE' }),
  batchSeats: (roomId, rows, cols) =>
    request('/seats/batch', { method: 'POST', params: { roomId, rows, cols } }),
  seatStatus: (id, status) => request(`/seats/${id}/status`, { method: 'PUT', params: { status } }),
  // 预约
  createBooking: (data) => request('/bookings', { method: 'POST', body: data }),
  myBookings: (params) => request('/bookings/mine', { params }),
  checkin: (id) => request(`/bookings/${id}/checkin`, { method: 'POST' }),
  checkinByCode: (code) => request('/bookings/checkin/code', { method: 'POST', params: { code } }),
  checkout: (id) => request(`/bookings/${id}/checkout`, { method: 'POST' }),
  cancelBooking: (id) => request(`/bookings/${id}/cancel`, { method: 'POST' }),
  // 打卡
  startCheckin: (seatId, bookingId) =>
    request('/checkins/start', { method: 'POST', params: { seatId, bookingId } }),
  endCheckin: (id) => request(`/checkins/${id}/end`, { method: 'POST' }),
  myCheckins: (params) => request('/checkins/mine', { params }),
  statistics: () => request('/checkins/statistics'),
  // 目标
  goals: () => request('/goals/mine'),
  createGoal: (data) => request('/goals', { method: 'POST', body: data }),
  updateGoal: (id, data) => request(`/goals/${id}`, { method: 'PUT', body: data }),
  deleteGoal: (id) => request(`/goals/${id}`, { method: 'DELETE' }),
  // 排行榜
  ranking: (period, topN = 10) =>
    request(`/ranking/${period}`, { params: { topN } }),
  myRank: (period) => request(`/ranking/my/${period}`),
  // 好友
  friends: () => request('/friends'),
  friendRequests: (params) => request('/friends/requests', { params }),
  requestFriend: (id) => request(`/friends/request/${id}`, { method: 'POST' }),
  acceptFriend: (id) => request(`/friends/accept/${id}`, { method: 'POST' }),
  rejectFriend: (id) => request(`/friends/reject/${id}`, { method: 'POST' }),
  unfriend: (id) => request(`/friends/${id}`, { method: 'DELETE' }),
  // 管理端
  adminDashboard: () => request('/admin/dashboard'),
  adminBookings: (params) => request('/admin/bookings', { params }),
  adminUtilization: () => request('/admin/utilization'),
  adminUserStats: () => request('/admin/user-stats'),
  adminViolations: (params) => request('/admin/violations', { params }),
  handleViolation: (id, status) =>
    request(`/admin/violations/${id}/handle`, { method: 'PUT', params: { status } }),
  adminUsers: (params) => request('/admin/users', { params }),
  adminCreateUser: (data) => request('/admin/users', { method: 'POST', body: data }),
  adminUpdateUser: (id, data) => request(`/admin/users/${id}`, { method: 'PUT', body: data }),
  adminDeleteUser: (id) => request(`/admin/users/${id}`, { method: 'DELETE' }),
  adminUserStatus: (id, status) =>
    request(`/admin/users/${id}/status`, { method: 'PUT', params: { status } }),
  adminLogs: (params) => request('/admin/users/logs', { params })
}
