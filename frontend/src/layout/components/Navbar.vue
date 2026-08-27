<template>
  <div class="navbar">
    <div class="breadcrumb">{{ $route.meta?.title }}</div>
    <el-dropdown class="user-dropdown">
      <span class="user-name">{{ userStore.nickName || userStore.name }}</span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.navbar { height:50px; display:flex; align-items:center; justify-content:space-between; padding:0 16px; background:#fff; box-shadow:0 1px 4px rgba(0,0,0,.1); }
.user-name { cursor:pointer; }
</style>
