<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="操作人员"><el-input v-model="query.operName" clearable /></el-form-item>
      <el-form-item label="业务类型">
        <el-select v-model="query.businessType" clearable style="width:140px">
          <el-option label="其他" :value="0" /><el-option label="新增" :value="1" /><el-option label="修改" :value="2" /><el-option label="删除" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作IP"><el-input v-model="query.operIp" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="日志编号" width="90" />
      <el-table-column prop="title" label="系统模块" width="140" />
      <el-table-column label="业务类型" width="90">
        <template #default="{ row }"><el-tag :type="['info','primary','warning','danger'][row.businessType] || 'info'">{{ ['其他','新增','修改','删除'][row.businessType] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="operName" label="操作人员" width="110" />
      <el-table-column prop="operIp" label="主机地址" width="140" />
      <el-table-column label="操作状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '成功' : '失败' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="operTime" label="操作时间" width="170" />
      <el-table-column prop="costTime" label="耗时(ms)" width="100" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">详细</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-drawer v-model="detailVisible" title="操作日志详情" size="500px">
      <el-descriptions :column="1" border v-if="detail">
        <el-descriptions-item label="系统模块">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="操作人员">{{ detail.operName }}</el-descriptions-item>
        <el-descriptions-item label="请求地址">{{ detail.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ detail.operIp }}</el-descriptions-item>
        <el-descriptions-item label="请求参数"><pre style="white-space:pre-wrap;">{{ detail.operParam }}</pre></el-descriptions-item>
        <el-descriptions-item label="返回结果"><pre style="white-space:pre-wrap;">{{ detail.jsonResult }}</pre></el-descriptions-item>
        <el-descriptions-item label="操作状态">{{ detail.status === 0 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item label="错误消息" v-if="detail.errorMsg">{{ detail.errorMsg }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detail.operTime }}</el-descriptions-item>
        <el-descriptions-item label="消耗时间">{{ detail.costTime }} ms</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { listLogPage, getLog, delLog } from '../../../api/system/log'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ operName: '', businessType: null, operIp: '', pageNo: 1, pageSize: 10 })
const detailVisible = ref(false)
const detail = ref(null)

async function fetchList() {
  loading.value = true
  try {
    const data = await listLogPage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.operName = ''; query.businessType = null; query.operIp = ''; query.pageNo = 1; fetchList() }

async function openDetail(row) {
  detail.value = await getLog(row.id)
  detailVisible.value = true
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条日志吗？`, '提示', { type: 'warning' }).then(async () => {
    await delLog(selected.value)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
