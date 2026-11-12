import { reactive, computed } from 'vue'
import { api } from './api'

const TOKEN_KEY = 'srb_token'
const USER_KEY = 'srb_user'

function readUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const store = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser(),
  pendingRequests: 0
})

export const isAdmin = computed(() => store.user && store.user.role === 1)
export const isLoggedIn = computed(() => !!store.token)

export function setSession(token, user) {
  store.token = token
  store.user = user
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function patchUser(partial) {
  store.user = { ...(store.user || {}), ...partial }
  localStorage.setItem(USER_KEY, JSON.stringify(store.user))
}

export function clearSession() {
  store.token = ''
  store.user = null
  store.pendingRequests = 0
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export async function refreshPendingRequests() {
  try {
    const res = await api.friendRequests({ current: 1, size: 1 })
    store.pendingRequests = res.total ?? (res.records ? res.records.length : 0)
  } catch {
    store.pendingRequests = 0
  }
}