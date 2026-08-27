<template>
  <template v-if="!item.children || item.children.length === 0">
    <el-menu-item :index="resolvePath(item.path)">
      <span>{{ item.meta?.title }}</span>
    </el-menu-item>
  </template>
  <el-sub-menu v-else :index="resolvePath(item.path)">
    <template #title><span>{{ item.meta?.title }}</span></template>
    <sidebar-item v-for="child in item.children" :key="child.path" :item="child" :base-path="resolvePath(item.path)" />
  </el-sub-menu>
</template>

<script setup>
const props = defineProps({ item: Object, basePath: String })
function resolvePath(p) {
  if (p.startsWith('/')) return p
  return `${props.basePath}/${p}`.replace(/\/+/g, '/')
}
</script>
