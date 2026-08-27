<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="公告标题"><el-input v-model="query.noticeTitle" clearable /></el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.noticeType" clearable style="width:120px"><el-option label="通知" value="1" /><el-option label="公告" value="2" /></el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:notice:add']" @click="openDialog()">新增</el-button>
      <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:notice:remove']" @click="handleBatchDelete">批量删除</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="noticeTitle" label="公告标题" />
      <el-table-column label="类型" width="90"><template #default="{ row }"><el-tag :type="row.noticeType === '1' ? 'warning' : 'success'">{{ row.noticeType === '1' ? '通知' : '公告' }}</el-tag></template></el-table-column>
      <el-table-column prop="createUser" label="创建者" width="120" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:notice:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="danger" v-hasPermi="['system:notice:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改公告' : '新增公告'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="noticeTitle"><el-input v-model="form.noticeTitle" /></el-form-item>
        <el-form-item label="类型" prop="noticeType"><el-radio-group v-model="form.noticeType"><el-radio value="1">通知</el-radio><el-radio value="2">公告</el-radio></el-radio-group></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">关闭</el-radio></el-radio-group></el-form-item>
        <el-form-item label="内容" prop="noticeContent"><editor v-model="form.noticeContent" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listNoticePage, addNotice, updateNotice, delNotice } from '../../../api/system/notice'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'
import Editor from '../../../components/Editor.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ noticeTitle: '', noticeType: null, pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, noticeTitle: '', noticeType: '1', status: 1, noticeContent: '' })
const rules = { noticeTitle: [{ required: true, message: '请输入公告标题', trigger: 'blur' }], noticeType: [{ required: true, message: '请选择类型', trigger: 'change' }], noticeContent: [{ required: true, message: '请输入内容', trigger: 'blur' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listNoticePage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.noticeTitle = ''; query.noticeType = null; query.pageNo = 1; fetchList() }

function openDialog(row) {
  Object.assign(form, { id: null, noticeTitle: '', noticeType: '1', status: 1, noticeContent: '' })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateNotice(form)
    else await addNotice(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除公告"${row.noticeTitle}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delNotice(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    await delNotice(selected.value)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
