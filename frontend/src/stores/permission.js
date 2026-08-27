import { defineStore } from 'pinia'
import request from '../utils/request'
import Layout from '../layout/index.vue'

const modules = import.meta.glob('../views/**/*.vue')

function resolveComponent(componentPath) {
  if (componentPath === 'Layout') return Layout
  const key = `../views/${componentPath}.vue`
  if (modules[key]) return modules[key]
  return () => import('../views/error/404.vue')
}

function convert(routerVOList) {
  return routerVOList.map((r) => {
    const route = {
      path: r.path,
      name: r.name,
      component: resolveComponent(r.component),
      meta: { title: r.meta?.title, icon: r.meta?.icon },
      children: r.children ? convert(r.children) : undefined
    }
    return route
  })
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({ routes: [] }),
  actions: {
    async generateRoutes() {
      const routerList = await request.get('/getRouters')
      const routes = convert(routerList)
      this.routes = routes
      return routes
    }
  }
})
