<template>
  <el-tag v-if="matched" :type="matched.elTagType || undefined">{{ matched.label }}</el-tag>
  <span v-else>{{ value }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ options: { type: Array, default: () => [] }, value: [String, Number] })
const matched = computed(() => props.options.find((o) => String(o.value) === String(props.value)))
</script>
