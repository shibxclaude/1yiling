<template>
  <div class="app-container">
    <el-form inline>
      <el-form-item label="用户名称"><el-input v-model="query.username" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button></el-form-item>
    </el-form>
    <el-row class="mb8">
      <el-button type="primary" @click="showSelectDialog = true">添加用户</el-button>
      <el-button type="danger" :disabled="!selected.length" @click="batchCancel">批量取消授权</el-button>
    </el-row>
    <el-table :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="username" label="用户名称" />
      <el-table-column prop="nick_name" label="用户昵称" />
      <el-table-column prop="phone" label="手机号码" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }"><el-button link type="danger" @click="cancel(row)">取消授权</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <select-user v-if="showSelectDialog" :role-id="roleId" @close="showSelectDialog = false" @confirm="onSelectConfirm" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { allocatedUserList, cancelUserRole, cancelUserRoleAll } from '../../../api/system/role'
import Pagination from '../../../components/Pagination.vue'
import SelectUser from './selectUser.vue'

const route = useRoute()
const roleId = Number(route.params.roleId)
const query = reactive({ roleId, username: '', pageNo: 1, pageSize: 10 })
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const showSelectDialog = ref(false)

async function fetchList() {
  const data = await allocatedUserList(query)
  tableData.value = data.records
  total.value = data.total
}

function cancel(row) {
  ElMessageBox.confirm(`确定取消用户"${row.username}"的角色授权吗？`, '提示', { type: 'warning' }).then(async () => {
    await cancelUserRole(row.id, roleId)
    ElMessage.success('已取消授权')
    fetchList()
  })
}

function batchCancel() {
  ElMessageBox.confirm('确定批量取消选中用户的角色授权吗？', '提示', { type: 'warning' }).then(async () => {
    await cancelUserRoleAll(roleId, selected.value)
    ElMessage.success('已取消授权')
    fetchList()
  })
}

function onSelectConfirm() { showSelectDialog.value = false; fetchList() }

onMounted(fetchList)
</script>
