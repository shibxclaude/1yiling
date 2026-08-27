<template>
  <div class="app-container">
    <el-card v-if="user">
      <p>用户：{{ user.username }}（{{ user.nickName }}）</p>
    </el-card>
    <el-checkbox-group v-model="checkedRoleIds" style="margin-top:16px;">
      <el-checkbox v-for="r in allRoles" :key="r.id" :value="r.id" :label="r.roleName" />
    </el-checkbox-group>
    <div style="margin-top:16px;"><el-button type="primary" @click="submit">保存</el-button></div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../../utils/request'

const route = useRoute()
const userId = Number(route.params.userId)
const user = ref(null)
const allRoles = ref([])
const checkedRoleIds = ref([])

onMounted(async () => {
  const detail = await request.post('/rest/sysUser/detailById', { id: userId })
  user.value = detail
  checkedRoleIds.value = detail.roleIds || []
  allRoles.value = (await request.post('/rest/sysRole/listPage', { pageNo: 1, pageSize: 100 })).records
})

async function submit() {
  const dto = { id: userId, username: user.value.username, nickName: user.value.nickName, deptId: user.value.deptId, roleIds: checkedRoleIds.value, postIds: user.value.postIds }
  await request.post('/rest/sysUser/update', dto)
  ElMessage.success('保存成功')
}
</script>
