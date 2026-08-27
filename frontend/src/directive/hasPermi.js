import { useUserStore } from '../stores/user'

export default {
  mounted(el, binding) {
    const { value } = binding
    const userStore = useUserStore()
    const permissions = userStore.permissions || []
    if (userStore.roles.includes('admin')) return
    if (Array.isArray(value) && value.length > 0) {
      const hasPermission = value.some((p) => permissions.includes(p))
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  }
}
