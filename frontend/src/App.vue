<template>
  <el-config-provider :size="appStore.size">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { useAppStore } from './stores/app'
const appStore = useAppStore()
</script>
