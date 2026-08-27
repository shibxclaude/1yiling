<template>
  <div class="navbar">
    <div class="breadcrumb">{{ $route.meta?.title }}</div>
    <div class="right-menu">
      <el-tooltip content="全屏">
        <el-icon class="right-menu-item" @click="toggleFullscreen"><FullScreen /></el-icon>
      </el-tooltip>
      <el-dropdown class="right-menu-item" @command="changeSize">
        <el-icon><Grid /></el-icon>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="large">大</el-dropdown-item>
            <el-dropdown-item command="default">默认</el-dropdown-item>
            <el-dropdown-item command="small">小</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-dropdown class="right-menu-item user-dropdown">
        <span class="user-name">{{ userStore.nickName || userStore.name }}</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
            <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import screenfull from 'screenfull'
import { FullScreen, Grid } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { useAppStore } from '../../stores/app'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

function toggleFullscreen() {
  if (screenfull.isEnabled) screenfull.toggle()
}

function changeSize(size) {
  appStore.size = size
}

function goProfile() {
  router.push('/user/profile')
}

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.navbar { height:50px; display:flex; align-items:center; justify-content:space-between; padding:0 16px; background:#fff; box-shadow:0 1px 4px rgba(0,0,0,.1); }
.right-menu { display:flex; align-items:center; gap:16px; }
.right-menu-item { cursor:pointer; font-size:18px; }
.user-name { cursor:pointer; }
</style>
