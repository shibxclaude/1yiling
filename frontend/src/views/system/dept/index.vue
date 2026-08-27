<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="部门名称"><el-input v-model="query.deptName" placeholder="请输入部门名称" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
          <el-option label="正常" :value="1" /><el-option label="停用" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:dept:add']" @click="openDialog()">新增</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" row-key="id" default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column prop="deptName" label="部门名称" width="220" />
      <el-table-column prop="orderNum" label="排序" width="80" />
      <el-table-column prop="leader" label="负责人" />
      <el-table-column prop="phone" label="联系电话" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:dept:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="primary" v-hasPermi="['system:dept:add']" @click="openDialog(null, row.id)">新增下级</el-button>
          <el-button link type="danger" v-hasPermi="['system:dept:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改部门' : '新增部门'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门" prop="parentId"><dept-tree-select v-model="form.parentId" /></el-form-item>
        <el-form-item label="部门编码" prop="deptCode"><el-input v-model="form.deptCode" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="部门名称" prop="deptName"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="显示顺序" prop="orderNum"><el-input-number v-model="form.orderNum" :min="0" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDept, addDept, updateDept, delDept } from '../../../api/system/dept'
import RightToolbar from '../../../components/RightToolbar.vue'
import DeptTreeSelect from '../../../components/DeptTreeSelect.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const query = reactive({ deptName: '', status: null })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, parentId: 0, deptCode: '', deptName: '', orderNum: 0, leader: '', phone: '', email: '', status: 1 })
const rules = { deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }], deptCode: [{ required: true, message: '请输入部门编码', trigger: 'blur' }], orderNum: [{ required: true, message: '请输入显示顺序', trigger: 'blur' }] }

function toTree(list) {
  const map = {}
  list.forEach((d) => { map[d.id] = { ...d, children: [] } })
  const roots = []
  list.forEach((d) => {
    if (d.parentId && map[d.parentId]) map[d.parentId].children.push(map[d.id])
    else roots.push(map[d.id])
  })
  return roots
}

async function fetchList() {
  loading.value = true
  try {
    const list = await listDept(query)
    tableData.value = toTree(list)
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.deptName = ''; query.status = null; fetchList() }

function openDialog(row, presetParentId) {
  Object.assign(form, { id: null, parentId: presetParentId || 0, deptCode: '', deptName: '', orderNum: 0, leader: '', phone: '', email: '', status: 1 })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateDept(form)
    else await addDept(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除部门"${row.deptName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delDept(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
