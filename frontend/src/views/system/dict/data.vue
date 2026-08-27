<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="字典标签"><el-input v-model="query.dictLabel" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:dict:add']" @click="openDialog()">新增</el-button>
      <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:dict:remove']" @click="handleBatchDelete">批量删除</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="dictLabel" label="字典标签" />
      <el-table-column prop="dictValue" label="字典键值" />
      <el-table-column prop="dictSort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:dict:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="danger" v-hasPermi="['system:dict:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改字典数据' : '新增字典数据'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="字典标签" prop="dictLabel"><el-input v-model="form.dictLabel" /></el-form-item>
        <el-form-item label="字典键值" prop="dictValue"><el-input v-model="form.dictValue" /></el-form-item>
        <el-form-item label="排序" prop="dictSort"><el-input-number v-model="form.dictSort" :min="0" /></el-form-item>
        <el-form-item label="样式属性"><el-input v-model="form.cssClass" /></el-form-item>
        <el-form-item label="回显样式"><el-input v-model="form.listClass" placeholder="primary/success/warning/danger/info" /></el-form-item>
        <el-form-item label="是否默认"><el-radio-group v-model="form.isDefault"><el-radio value="Y">是</el-radio><el-radio value="N">否</el-radio></el-radio-group></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDictDataPage, addDictData, updateDictData, delDictData } from '../../../api/system/dict'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const route = useRoute()
const dictType = route.params.dictType
const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ dictType, dictLabel: '', pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, dictType, dictLabel: '', dictValue: '', dictSort: 0, cssClass: '', listClass: '', isDefault: 'N', status: 1, remark: '' })
const rules = { dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }], dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }], dictSort: [{ required: true, message: '请输入排序', trigger: 'blur' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listDictDataPage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.dictLabel = ''; query.pageNo = 1; fetchList() }

function openDialog(row) {
  Object.assign(form, { id: null, dictType, dictLabel: '', dictValue: '', dictSort: 0, cssClass: '', listClass: '', isDefault: 'N', status: 1, remark: '' })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateDictData(form)
    else await addDictData(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除字典数据"${row.dictLabel}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delDictData(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    await delDictData(selected.value)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
