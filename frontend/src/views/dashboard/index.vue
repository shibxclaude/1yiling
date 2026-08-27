<template>
  <div class="app-container">
    <div class="welcome">
      <div class="text">
        <p class="eyebrow">欢迎回来</p>
        <h2 class="name">{{ userStore.nickName || userStore.name }}</h2>
        <p class="hint">今天也要顺利完成系统管理工作</p>
      </div>
      <div class="avatar-wrap">
        <el-avatar :size="56" :src="userStore.avatar || undefined">
          {{ (userStore.nickName || userStore.name || '?').slice(0, 1) }}
        </el-avatar>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useUserStore } from '../../stores/user'
const userStore = useUserStore()
</script>

<style scoped>
.welcome { display: flex; align-items: center; justify-content: space-between; }
.eyebrow { color: var(--y-text-secondary); font-size: 13px; margin: 0 0 4px; }
.name { margin: 0; font-size: 22px; color: var(--y-text-primary); }
.hint { margin: 6px 0 0; color: var(--y-text-secondary); font-size: 13px; }
.avatar-wrap :deep(.el-avatar) { background: var(--y-accent); box-shadow: var(--y-shadow-accent); font-weight: 600; }
</style>
