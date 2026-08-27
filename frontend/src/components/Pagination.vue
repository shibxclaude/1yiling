<template>
  <el-pagination
    v-model:current-page="pageNo"
    v-model:page-size="pageSize"
    :total="total"
    :page-sizes="[10, 20, 50, 100]"
    layout="total, sizes, prev, pager, next, jumper"
    style="margin-top:12px; justify-content:flex-end; display:flex;"
    @size-change="emitChange"
    @current-change="emitChange"
  />
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ page: { type: Number, default: 1 }, limit: { type: Number, default: 10 }, total: { type: Number, default: 0 } })
const emit = defineEmits(['update:page', 'update:limit', 'pagination'])

const pageNo = ref(props.page)
const pageSize = ref(props.limit)

watch(() => props.page, (v) => { pageNo.value = v })
watch(() => props.limit, (v) => { pageSize.value = v })

function emitChange() {
  emit('update:page', pageNo.value)
  emit('update:limit', pageSize.value)
  emit('pagination', { pageNo: pageNo.value, pageSize: pageSize.value })
}
</script>
