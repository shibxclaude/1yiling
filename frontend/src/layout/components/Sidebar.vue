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
  color: var(--y-text-primary);
  font-weight: 600;
  letter-spacing: 0.3px;
  border-bottom: 1px solid var(--y-border);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--y-accent);
  box-shadow: 0 0 0 4px var(--y-accent-soft);
}
.sidebar-menu { height: calc(100% - 50px); border-right: none; padding: 10px; }
.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  border-radius: var(--y-radius-sm);
  margin-bottom: 3px;
  height: 44px;
  line-height: 44px;
  transition: background 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
/* hover: a visible accent tint. This used to be near-white (#fafbfc),
   which made a just-clicked item look unchanged since the cursor stays
   on it after the click. */
.sidebar-menu :deep(.el-menu-item:not(.is-active):hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: var(--y-sidebar-bg-hover) !important;
  color: var(--y-accent) !important;
}

/* the active item is a solid dark-accent pill that lifts off the white
   sidebar via a colored shadow — 立体感 comes from color + shadow together.
   Nested (second-level) items live in .el-sub-menu > .el-menu, so they are
   matched explicitly here, and the :hover variants are pinned to the same
   dark fill so hovering a selected item never washes it back out. */
.sidebar-menu :deep(.el-menu-item.is-active),
.sidebar-menu :deep(.el-menu-item.is-active:hover),
.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active),
.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active:hover) {
  background: var(--y-sidebar-active-bg) !important;
  background-color: var(--y-sidebar-active-bg) !important;
  color: var(--y-sidebar-text-active) !important;
  box-shadow: var(--y-shadow-accent);
  font-weight: 600;
  transform: translateY(-1px);
}
.sidebar-menu :deep(.el-menu-item.is-active .el-icon),
.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active .el-icon) {
  color: var(--y-sidebar-text-active) !important;
}

/* an open submenu's parent title stays tinted so the active branch is
   traceable from the top level down */
.sidebar-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--y-accent) !important;
  font-weight: 600;
}
</style>
