<template>
  <div class="sidebar-brand">
    <span class="dot" /> <span class="name">yiling manager</span>
  </div>
  <el-menu :default-active="$route.path" router unique-opened class="sidebar-menu" background-color="transparent" text-color="var(--y-sidebar-text)" active-text-color="var(--y-sidebar-text-active)">
    <sidebar-item v-for="route in routes" :key="route.path" :item="route" :base-path="route.path" />
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { usePermissionStore } from '../../stores/permission'
import SidebarItem from './SidebarItem.vue'

const permissionStore = usePermissionStore()
const routes = computed(() => permissionStore.routes)
</script>

<style scoped>
.sidebar-brand {
  height: 50px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.3px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--y-accent);
  box-shadow: 0 0 0 4px rgba(91, 95, 239, 0.25);
}
.sidebar-menu { height: calc(100% - 50px); border-right: none; padding: 8px; }
.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  border-radius: var(--y-radius-sm);
  margin-bottom: 2px;
  height: 44px;
  line-height: 44px;
}
.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: var(--y-sidebar-bg-hover) !important;
}
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--y-sidebar-active) !important;
  position: relative;
}
.sidebar-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--y-accent);
}
</style>
