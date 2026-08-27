<template>
  <div class="tags-view">
    <span
      v-for="tag in visitedViews"
      :key="tag.path"
      class="tag-item"
      :class="{ active: tag.path === $route.path }"
      @click="go(tag.path)"
    >
      {{ tag.title }}
      <el-icon class="tag-close" @click.stop="close(tag)"><Close /></el-icon>
    </span>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const visitedViews = ref([])

watch(() => route.path, () => {
  if (route.meta?.title && !visitedViews.value.find((v) => v.path === route.path)) {
    visitedViews.value.push({ path: route.path, title: route.meta.title })
  }
}, { immediate: true })

function go(path) { router.push(path) }
function close(tag) {
  visitedViews.value = visitedViews.value.filter((v) => v.path !== tag.path)
  if (route.path === tag.path && visitedViews.value.length) {
    router.push(visitedViews.value[visitedViews.value.length - 1].path)
  }
}
</script>

<style scoped>
.tags-view {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--y-surface);
  border-bottom: 1px solid var(--y-border);
  overflow-x: auto;
}
.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: var(--y-radius-sm);
  font-size: 13px;
  color: var(--y-text-secondary);
  background: var(--y-surface-sunken);
  border: 1px solid var(--y-border);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}
.tag-item:hover { color: var(--y-accent); border-color: var(--y-accent); }
.tag-item.active {
  color: #fff;
  background: var(--y-accent);
  border-color: var(--y-accent);
  box-shadow: var(--y-shadow-accent);
}
.tag-close { font-size: 12px; }
.tag-close:hover { opacity: 0.7; }
</style>
