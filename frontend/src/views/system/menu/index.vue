<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="菜单名称"><el-input v-model="query.menuName" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width:120px">
          <el-option label="正常" :value="1" /><el-option label="隐藏" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:menu:add']" @click="openDialog()">新增</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" row-key="id" default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column prop="menuName" label="菜单名称" width="220" />
      <el-table-column label="图标" width="70"><template #default="{ row }"><component :is="row.icon" v-if="row.icon" style="width:18px" /></template></el-table-column>
      <el-table-column prop="menuSort" label="排序" width="70" />
      <el-table-column prop="perms" label="权限标识" />
      <el-table-column prop="menuComponent" label="组件路径" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '隐藏' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:menu:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="primary" v-hasPermi="['system:menu:add']" @click="openDialog(null, row.id)">新增下级</el-button>
          <el-button link type="danger" v-hasPermi="['system:menu:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改菜单' : '新增菜单'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单"><el-input :model-value="form.parentId" disabled /></el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.menuType">
            <el-radio value="0">目录</el-radio><el-radio value="1">菜单</el-radio><el-radio value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图标" v-if="form.menuType !== '2'"><icon-select v-model="form.icon" /></el-form-item>
        <el-form-item label="名称" prop="menuName"><el-input v-model="form.menuName" /></el-form-item>
        <el-form-item label="排序" prop="menuSort"><el-input-number v-model="form.menuSort" :min="0" /></el-form-item>
        <template v-if="form.menuType !== '2'">
          <el-form-item label="是否外链"><el-switch v-model="form.ifFrame" /></el-form-item>
          <el-form-item label="路由地址" v-if="form.menuType === '1'"><el-input v-model="form.menuPath" /></el-form-item>
          <el-form-item label="组件路径" v-if="form.menuType === '1'"><el-input v-model="form.menuComponent" placeholder="如 system/user/index" /></el-form-item>
          <el-form-item label="路由参数"><el-input v-model="form.queryParam" /></el-form-item>
          <el-form-item label="是否缓存" v-if="form.menuType === '1'"><el-switch v-model="form.ifCache" /></el-form-item>
        </template>
        <el-form-item label="权限字符" v-if="form.menuType !== '0'"><el-input v-model="form.perms" placeholder="system:user:add" /></el-form-item>
        <el-form-item label="显示状态">
          <el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">隐藏</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMenu, addMenu, updateMenu, delMenu } from '../../../api/system/menu'
import RightToolbar from '../../../components/RightToolbar.vue'
import IconSelect from '../../../components/IconSelect.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const query = reactive({ menuName: '', status: null })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, parentId: 0, menuName: '', menuType: '1', menuSort: 0, icon: '', menuPath: '', menuComponent: '', perms: '', queryParam: '', ifFrame: false, ifCache: false, status: 1 })
const rules = { menuName: [{ required: true, message: '请输入名称', trigger: 'blur' }], menuSort: [{ required: true, message: '请输入排序', trigger: 'blur' }] }

function toTree(list) {
  const map = {}
  list.forEach((m) => { map[m.id] = { ...m, children: [] } })
  const roots = []
  list.forEach((m) => {
    if (m.parentId && map[m.parentId]) map[m.parentId].children.push(map[m.id])
    else roots.push(map[m.id])
  })
  return roots
}

async function fetchList() {
  loading.value = true
  try {
    const list = await listMenu(query)
    tableData.value = toTree(list)
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.menuName = ''; query.status = null; fetchList() }

function openDialog(row, presetParentId) {
  Object.assign(form, { id: null, parentId: presetParentId || 0, menuName: '', menuType: '1', menuSort: 0, icon: '', menuPath: '', menuComponent: '', perms: '', queryParam: '', ifFrame: false, ifCache: false, status: 1 })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateMenu(form)
    else await addMenu(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除菜单"${row.menuName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delMenu(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
