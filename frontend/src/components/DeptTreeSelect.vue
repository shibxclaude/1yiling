<template>
  <el-tree-select
    v-model="innerValue"
    :data="deptTree"
    :props="{ label: 'label', children: 'children', value: 'id' }"
    node-key="id"
    check-strictly
    placeholder="选择归属部门"
    style="width:100%"
    @change="(v) => $emit('update:modelValue', v)"
  />
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import request from '../utils/request'

const props = defineProps({ modelValue: [Number, String] })
const emit = defineEmits(['update:modelValue'])
const innerValue = ref(props.modelValue)
const deptTree = ref([])

watch(() => props.modelValue, (v) => { innerValue.value = v })

onMounted(async () => {
  deptTree.value = await request.post('/rest/sysDept/deptTree', {})
})
</script>
