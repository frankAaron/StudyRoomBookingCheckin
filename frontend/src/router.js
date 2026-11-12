import { createRouter, createWebHashHistory } from 'vue-router'
import { store } from './store'

const routes = [
  { path: '/login', name: 'login', component: () => import('./views/Login.vue'), meta: { public: true } },
  { path: '/', redirect: '/home' },
  { path: '/home', name: 'home', component: () => import('./views/Home.vue'), meta: { title: '首页' } },
  { path: '/rooms', name: 'rooms', component: () => import('./views/Rooms.vue'), meta: { title: '自习室与座位' } },
  { path: '/bookings', name: 'bookings', component: () => import('./views/Bookings.vue'), meta: { title: '我的预约' } },
  { path: '/study', name: 'study', component: () => import('./views/Study.vue'), meta: { title: '学习计时' } },
  { path: '/stats', name: 'stats', component: () => import('./views/Stats.vue'), meta: { title: '学习统计' } },
  { path: '/goals', name: 'goals', component: () => import('./views/Goals.vue'), meta: { title: '学习目标' } },
  { path: '/ranking', name: 'ranking', component: () => import('./views/Ranking.vue'), meta: { title: '排行榜' } },
  { path: '/admin', name: 'admin', component: () => import('./views/Admin.vue'), meta: { title: '管理后台', admin: true } },
  { path: '/users', name: 'users', component: () => import('./views/Users.vue'), meta: { title: '用户管理', admin: true } },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) {
    if (store.token && to.name === 'login') return { name: 'home' }
    return true
  }
  if (!store.token) {
    return { name: 'login' }
  }
  if (to.meta.admin && (!store.user || store.user.role !== 1)) {
    return { name: 'home' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 自习室预约系统` : '自习室预约与学习打卡系统'
})

export default router
