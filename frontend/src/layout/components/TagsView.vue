<template>
  <div class="tags-view">
    <el-tag v-for="tag in visitedViews" :key="tag.path" closable class="tag-item" :type="tag.path === $route.path ? '' : 'info'" @click="go(tag.path)" @close="close(tag)">
      {{ tag.title }}
    </el-tag>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

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
.tags-view { padding:6px 12px; background:#fff; border-bottom:1px solid #eee; }
.tag-item { margin-right:8px; cursor:pointer; }
</style>
