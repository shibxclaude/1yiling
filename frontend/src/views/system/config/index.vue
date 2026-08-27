<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="参数名称"><el-input v-model="query.configName" clearable /></el-form-item>
      <el-form-item label="参数键名"><el-input v-model="query.configKey" clearable /></el-form-item>
      <el-form-item label="内置">
        <el-select v-model="query.configType" clearable style="width:120px"><el-option label="是" value="Y" /><el-option label="否" value="N" /></el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:config:add']" @click="openDialog()">新增</el-button>
      <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:config:remove']" @click="handleBatchDelete">批量删除</el-button>
      <el-button @click="handleRefresh">刷新缓存</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="configName" label="参数名称" />
      <el-table-column prop="configKey" label="参数键名" />
      <el-table-column prop="configValue" label="参数键值" />
      <el-table-column label="内置" width="80"><template #default="{ row }">{{ row.configType === 'Y' ? '是' : '否' }}</template></el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:config:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="danger" v-hasPermi="['system:config:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改参数' : '新增参数'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数名称" prop="configName"><el-input v-model="form.configName" /></el-form-item>
        <el-form-item label="参数键名" prop="configKey"><el-input v-model="form.configKey" /></el-form-item>
        <el-form-item label="参数键值" prop="configValue"><el-input v-model="form.configValue" /></el-form-item>
        <el-form-item label="是否内置"><el-radio-group v-model="form.configType"><el-radio value="Y">是</el-radio><el-radio value="N">否</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listConfigPage, addConfig, updateConfig, delConfig, refreshConfigCache } from '../../../api/system/config'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ configName: '', configKey: '', configType: null, pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, configName: '', configKey: '', configValue: '', configType: 'N', remark: '' })
const rules = { configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }], configKey: [{ required: true, message: '请输入参数键名', trigger: 'blur' }], configValue: [{ required: true, message: '请输入参数键值', trigger: 'blur' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listConfigPage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.configName = ''; query.configKey = ''; query.configType = null; query.pageNo = 1; fetchList() }

function openDialog(row) {
  Object.assign(form, { id: null, configName: '', configKey: '', configValue: '', configType: 'N', remark: '' })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateConfig(form)
    else await addConfig(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除参数"${row.configName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delConfig(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    await delConfig(selected.value)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleRefresh() {
  refreshConfigCache().then(() => ElMessage.success('刷新成功'))
}

onMounted(fetchList)
</script>
