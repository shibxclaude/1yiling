<template>
  <el-popover trigger="click" width="320">
    <template #reference>
      <el-input v-model="innerValue" placeholder="点击选择图标" readonly>
        <template #prefix><component :is="innerValue" v-if="innerValue" style="width:16px" /></template>
      </el-input>
    </template>
    <div class="icon-grid">
      <div v-for="name in iconNames" :key="name" class="icon-cell" @click="select(name)">
        <component :is="name" style="width:20px" />
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, watch } from 'vue'
import * as Icons from '@element-plus/icons-vue'

const props = defineProps({ modelValue: String })
const emit = defineEmits(['update:modelValue'])
const innerValue = ref(props.modelValue)
const iconNames = Object.keys(Icons)

watch(() => props.modelValue, (v) => { innerValue.value = v })

function select(name) {
  innerValue.value = name
  emit('update:modelValue', name)
}
</script>

<style scoped>
.icon-grid { display:grid; grid-template-columns:repeat(8,1fr); gap:8px; max-height:240px; overflow-y:auto; }
.icon-cell { cursor:pointer; display:flex; justify-content:center; padding:4px; }
.icon-cell:hover { background:#f0f2f5; }
</style>
