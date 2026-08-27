<template>
  <div class="right-toolbar">
    <el-tooltip content="刷新">
      <el-button circle :icon="Refresh" @click="$emit('queryTable')" />
    </el-tooltip>
    <el-tooltip content="隐藏/显示搜索">
      <el-button circle :icon="Search" @click="toggleSearch" />
    </el-tooltip>
  </div>
</template>

<script setup>
import { Refresh, Search } from '@element-plus/icons-vue'

const props = defineProps({ showSearch: { type: Boolean, default: true } })
const emit = defineEmits(['update:showSearch', 'queryTable'])

function toggleSearch() {
  emit('update:showSearch', !props.showSearch)
}
</script>

<style scoped>
.right-toolbar { display:flex; gap:8px; float:right; }
</style>
