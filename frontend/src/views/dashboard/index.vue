<template>
  <el-card>
    <h2>欢迎使用 yiling-manager 后台管理系统</h2>
    <p>当前登录：{{ userStore.nickName || userStore.name }}</p>
  </el-card>
</template>

<script setup>
import { useUserStore } from '../../stores/user'
const userStore = useUserStore()
</script>
