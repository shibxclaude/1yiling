<template>
  <el-menu :default-active="$route.path" router unique-opened class="sidebar-menu" background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF">
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
.sidebar-menu { height: 100%; border-right: none; }
</style>
