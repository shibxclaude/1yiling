<template>
  <el-dialog model-value title="选择用户" width="700px" @close="$emit('close')">
    <el-form inline><el-form-item label="用户名称"><el-input v-model="query.username" clearable /></el-form-item><el-form-item><el-button type="primary" @click="fetchList">搜索</el-button></el-form-item></el-form>
    <el-table :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="username" label="用户名称" />
      <el-table-column prop="nick_name" label="用户昵称" />
      <el-table-column prop="phone" label="手机号码" />
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />
    <template #footer><el-button @click="$emit('close')">取消</el-button><el-button type="primary" @click="confirm">确定</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { unallocatedUserList, selectUserRoleAll } from '../../../api/system/role'
import Pagination from '../../../components/Pagination.vue'

const props = defineProps({ roleId: Number })
const emit = defineEmits(['close', 'confirm'])
const query = reactive({ roleId: props.roleId, username: '', pageNo: 1, pageSize: 10 })
const tableData = ref([])
const total = ref(0)
const selected = ref([])

async function fetchList() {
  const data = await unallocatedUserList(query)
  tableData.value = data.records
  total.value = data.total
}

async function confirm() {
  if (!selected.value.length) { ElMessage.warning('请至少选择一个用户'); return }
  await selectUserRoleAll(props.roleId, selected.value)
  ElMessage.success('添加成功')
  emit('confirm')
}

onMounted(fetchList)
</script>
