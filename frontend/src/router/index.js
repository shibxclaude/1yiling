import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'
import { useUserStore } from '../stores/user'
import { usePermissionStore } from '../stores/permission'
import Layout from '../layout/index.vue'

export const constantRoutes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/index.vue') },
  { path: '/404', name: '404', component: () => import('../views/error/404.vue') },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/index.vue'), meta: { title: '首页' } },
      { path: 'system/role-auth-user/:roleId', name: 'RoleAuthUser', component: () => import('../views/system/role/authUser.vue'), meta: { title: '分配用户' } },
      { path: 'system/user-auth-role/:userId', name: 'UserAuthRole', component: () => import('../views/system/user/authRole.vue'), meta: { title: '分配角色' } },
      { path: 'user/profile', name: 'Profile', component: () => import('../views/system/user/profile/index.vue'), meta: { title: '个人中心' } },
      { path: 'system/dict-data/:dictType', name: 'DictData', component: () => import('../views/system/dict/data.vue'), meta: { title: '字典数据' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

let dynamicRoutesLoaded = false

router.beforeEach(async (to, from, next) => {
  const hasToken = getToken()
  if (to.path === '/login') {
    next()
    return
  }
  if (!hasToken) {
    next('/login')
    return
  }
  const userStore = useUserStore()
  if (!userStore.name) {
    try {
      await userStore.getInfo()
    } catch {
      userStore.logout()
      next('/login')
      return
    }
  }
  if (!dynamicRoutesLoaded) {
    const permissionStore = usePermissionStore()
    const routes = await permissionStore.generateRoutes()
    // Each top-level RouterVO is a menu *directory* and already carries
    // component: Layout itself (see stores/permission.js) — these must be
    // added as sibling top-level routes, NOT as children of the constantRoutes
    // '/' Layout route, or Layout would render doubly-nested.
    routes.forEach((r) => router.addRoute(r))
    dynamicRoutesLoaded = true
    next({ ...to, replace: true })
    return
  }
  next()
})

export default router
