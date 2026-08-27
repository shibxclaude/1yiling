<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="字典名称"><el-input v-model="query.dictName" clearable /></el-form-item>
      <el-form-item label="字典类型"><el-input v-model="query.dictType" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:dict:add']" @click="openDialog()">新增</el-button>
      <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:dict:remove']" @click="handleBatchDelete">批量删除</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="dictName" label="字典名称" />
      <el-table-column prop="dictType" label="字典类型">
        <template #default="{ row }"><el-link type="primary" @click="goData(row)">{{ row.dictType }}</el-link></template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:dict:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="danger" v-hasPermi="['system:dict:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改字典类型' : '新增字典类型'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName"><el-input v-model="form.dictName" /></el-form-item>
        <el-form-item label="字典类型" prop="dictType"><el-input v-model="form.dictType" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="状态" v-if="form.id"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group></el-form-item>
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
import { listDictTypePage, addDictType, updateDictType, delDictType } from '../../../api/system/dict'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const router = useRouter()
const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ dictName: '', dictType: '', pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, dictName: '', dictType: '', status: 1, remark: '' })
const rules = { dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }], dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listDictTypePage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.dictName = ''; query.dictType = ''; query.pageNo = 1; fetchList() }

function openDialog(row) {
  Object.assign(form, { id: null, dictName: '', dictType: '', status: 1, remark: '' })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateDictType(form)
    else await addDictType(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function goData(row) { router.push(`/system/dict-data/${row.dictType}`) }

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除字典"${row.dictName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delDictType(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    await delDictType(selected.value)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
