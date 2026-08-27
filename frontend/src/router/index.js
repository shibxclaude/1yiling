import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'

export const constantRoutes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/index.vue') },
  { path: '/404', name: '404', component: () => import('../views/error/404.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

router.beforeEach((to, from, next) => {
  const hasToken = getToken()
  if (to.path === '/login') {
    next()
    return
  }
  if (!hasToken) {
    next('/login')
    return
  }
  next()
})

export default router
