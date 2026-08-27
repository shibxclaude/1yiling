<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="角色名称"><el-input v-model="query.roleName" clearable /></el-form-item>
      <el-form-item label="权限字符"><el-input v-model="query.roleKey" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width:120px"><el-option label="正常" :value="1" /><el-option label="停用" :value="2" /></el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:role:add']" @click="openDialog()">新增</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData">
      <el-table-column prop="id" label="角色编号" width="90" />
      <el-table-column prop="roleName" label="角色名称" />
      <el-table-column prop="roleKey" label="权限字符" />
      <el-table-column prop="roleSort" label="显示顺序" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" :disabled="row.roleKey === 'admin'" @change="(v) => toggleStatus(row, v)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:role:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="primary" v-hasPermi="['system:role:edit']" @click="openDataScope(row)">数据权限</el-button>
          <el-button link type="primary" @click="goAuthUser(row)">分配用户</el-button>
          <el-button link type="danger" v-hasPermi="['system:role:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改角色' : '新增角色'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="权限字符" prop="roleKey"><el-input v-model="form.roleKey" /></el-form-item>
        <el-form-item label="显示顺序" prop="roleSort"><el-input-number v-model="form.roleSort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="菜单权限">
          <el-tree ref="menuTreeRef" :data="menuTree" show-checkbox node-key="id" :props="{ label: 'label', children: 'children' }" style="max-height:260px; overflow-y:auto; border:1px solid #dcdfe6; width:100%; padding:8px;" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="dataScopeVisible" title="分配数据权限" width="500px">
      <el-form label-width="90px">
        <el-form-item label="数据范围">
          <el-radio-group v-model="dataScopeForm.dataScope">
            <el-radio value="1">全部数据权限</el-radio><el-radio value="2">自定义数据权限</el-radio>
            <el-radio value="3">本部门数据权限</el-radio><el-radio value="4">本部门及以下数据权限</el-radio><el-radio value="5">仅本人数据权限</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数据权限" v-if="dataScopeForm.dataScope === '2'">
          <el-tree ref="deptTreeRef" :data="deptTree" show-checkbox node-key="id" :props="{ label: 'label', children: 'children' }" style="max-height:260px; overflow-y:auto; border:1px solid #dcdfe6; width:100%; padding:8px;" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dataScopeVisible = false">取消</el-button><el-button type="primary" @click="submitDataScope">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRolePage, addRole, updateRole, delRole, updateRoleStatus, roleMenuTreeSelect, deptTreeByRole, saveDataScope } from '../../../api/system/role'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const router = useRouter()
const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ roleName: '', roleKey: '', status: null, pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const menuTreeRef = ref()
const menuTree = ref([])
const form = reactive({ id: null, roleName: '', roleKey: '', roleSort: 0, status: 1, remark: '' })
const rules = { roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }], roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }] }

const dataScopeVisible = ref(false)
const deptTreeRef = ref()
const deptTree = ref([])
const dataScopeForm = reactive({ id: null, dataScope: '1' })

async function fetchList() {
  loading.value = true
  try {
    const data = await listRolePage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.roleName = ''; query.roleKey = ''; query.status = null; query.pageNo = 1; fetchList() }

async function openDialog(row) {
  Object.assign(form, { id: null, roleName: '', roleKey: '', roleSort: 0, status: 1, remark: '' })
  if (row) Object.assign(form, row)
  const treeData = await roleMenuTreeSelect(row ? row.id : 0)
  menuTree.value = treeData.menus
  dialogVisible.value = true
  setTimeout(() => menuTreeRef.value?.setCheckedKeys(treeData.checkedKeys || []), 0)
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    const menuIds = [...(menuTreeRef.value?.getCheckedKeys() || []), ...(menuTreeRef.value?.getHalfCheckedKeys() || [])]
    const payload = { ...form, menuIds }
    if (form.id) await updateRole(payload)
    else await addRole(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

async function openDataScope(row) {
  dataScopeForm.id = row.id
  dataScopeForm.dataScope = row.dataScope || '1'
  const data = await deptTreeByRole(row.id)
  deptTree.value = data.depts
  dataScopeVisible.value = true
  setTimeout(() => deptTreeRef.value?.setCheckedKeys(data.checkedKeys || []), 0)
}

async function submitDataScope() {
  const deptIdList = dataScopeForm.dataScope === '2' ? (deptTreeRef.value?.getCheckedKeys() || []) : []
  await saveDataScope({ id: dataScopeForm.id, dataScope: dataScopeForm.dataScope, deptIdList })
  ElMessage.success('保存成功')
  dataScopeVisible.value = false
  fetchList()
}

function toggleStatus(row, checked) {
  updateRoleStatus(row.id, checked ? 1 : 2).then(() => { row.status = checked ? 1 : 2; ElMessage.success('状态已更新') })
}

function goAuthUser(row) { router.push(`/system/role-auth-user/${row.id}`) }

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除角色"${row.roleName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delRole(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
