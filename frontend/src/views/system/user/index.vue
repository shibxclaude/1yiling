<template>
  <div class="app-container">
    <el-row :gutter="16">
      <el-col :span="4">
        <dept-tree-select v-model="query.deptId" @update:model-value="fetchList" />
      </el-col>
      <el-col :span="20">
        <el-form v-show="showSearch" :model="query" inline>
          <el-form-item label="用户名称"><el-input v-model="query.username" clearable /></el-form-item>
          <el-form-item label="手机号码"><el-input v-model="query.phone" clearable /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable style="width:120px"><el-option label="正常" :value="1" /><el-option label="停用" :value="2" /></el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
        </el-form>

        <el-row class="mb8">
          <el-button type="primary" v-hasPermi="['system:user:add']" @click="openDialog()">新增</el-button>
          <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:user:remove']" @click="handleBatchDelete">批量删除</el-button>
          <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
        </el-row>

        <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="id" label="编号" width="80" />
          <el-table-column prop="username" label="用户名称" />
          <el-table-column prop="nickName" label="用户昵称" />
          <el-table-column prop="deptName" label="部门" />
          <el-table-column prop="phone" label="手机号码" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-switch :model-value="row.status === 1" :disabled="row.username === 'admin'" @change="(v) => toggleStatus(row, v)" /></template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button link type="primary" v-hasPermi="['system:user:edit']" @click="openDialog(row)">修改</el-button>
              <el-button link type="danger" v-hasPermi="['system:user:remove']" @click="handleDelete(row)">删除</el-button>
              <el-dropdown>
                <el-button link type="primary">更多<el-icon><arrow-down /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="openResetPwd(row)">重置密码</el-dropdown-item>
                    <el-dropdown-item @click="goAuthRole(row)">分配角色</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改用户' : '新增用户'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户昵称" prop="nickName"><el-input v-model="form.nickName" /></el-form-item>
        <el-form-item label="归属部门" prop="deptId"><dept-tree-select v-model="form.deptId" /></el-form-item>
        <el-form-item label="手机号码"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="用户名称" prop="username"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="密码" prop="passwd" v-if="!form.id"><el-input v-model="form.passwd" type="password" show-password /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.sex" style="width:120px"><el-option label="男" value="0" /><el-option label="女" value="1" /><el-option label="未知" value="2" /></el-select>
        </el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.postIds" multiple style="width:100%"><el-option v-for="p in postOptions" :key="p.id" :label="p.postName" :value="p.id" /></el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width:100%"><el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" /></el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { listUserPage, addUser, updateUser, delUser, updateUserStatus, resetUserPwd } from '../../../api/system/user'
import { listPost } from '../../../api/system/post'
import request from '../../../utils/request'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'
import DeptTreeSelect from '../../../components/DeptTreeSelect.vue'

const router = useRouter()
const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const postOptions = ref([])
const roleOptions = ref([])
const query = reactive({ deptId: null, username: '', phone: '', status: null, pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, deptId: null, username: '', nickName: '', passwd: '', sex: '2', email: '', phone: '', status: 1, remark: '', roleIds: [], postIds: [] })
const rules = { username: [{ required: true, message: '请输入用户名称', trigger: 'blur' }], nickName: [{ required: true, message: '请输入用户昵称', trigger: 'blur' }], passwd: [{ required: true, message: '请输入密码', trigger: 'blur' }], deptId: [{ required: true, message: '请选择归属部门', trigger: 'change' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listUserPage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.username = ''; query.phone = ''; query.status = null; query.pageNo = 1; fetchList() }

async function openDialog(row) {
  Object.assign(form, { id: null, deptId: null, username: '', nickName: '', passwd: '', sex: '2', email: '', phone: '', status: 1, remark: '', roleIds: [], postIds: [] })
  if (row) {
    const detail = await request.post('/rest/sysUser/detailById', { id: row.id })
    Object.assign(form, detail, { roleIds: detail.roleIds, postIds: detail.postIds })
  }
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateUser(form)
    else await addUser(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function toggleStatus(row, checked) {
  updateUserStatus(row.id, checked ? 1 : 2).then(() => { row.status = checked ? 1 : 2; ElMessage.success('状态已更新') })
}

function openResetPwd(row) {
  ElMessageBox.prompt('请输入新密码（5-20位）', '重置密码', {
    inputPattern: /^.{5,20}$/,
    inputErrorMessage: '密码长度必须在5-20位之间'
  }).then(async ({ value }) => {
    await resetUserPwd(row.id, value)
    ElMessage.success('密码重置成功')
  })
}

function goAuthRole(row) { router.push(`/system/user-auth-role/${row.id}`) }

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除用户"${row.username}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delUser(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    for (const id of selected.value) await delUser(id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(async () => {
  fetchList()
  postOptions.value = await listPost()
  roleOptions.value = (await request.post('/rest/sysRole/listPage', { pageNo: 1, pageSize: 100 })).records
})
</script>
