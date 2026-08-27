<template>
  <el-row :gutter="16">
    <el-col :span="8">
      <el-card>
        <user-avatar />
        <p>用户名：{{ profile?.username }}</p>
        <p>手机号：{{ profile?.phone }}</p>
        <p>邮箱：{{ profile?.email }}</p>
        <p>创建日期：{{ profile?.createTime }}</p>
      </el-card>
    </el-col>
    <el-col :span="16">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本资料" name="info"><user-info :profile="profile" @updated="fetchProfile" /></el-tab-pane>
        <el-tab-pane label="修改密码" name="pwd"><reset-pwd /></el-tab-pane>
      </el-tabs>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getProfile } from '../../../../api/system/user'
import UserAvatar from './userAvatar.vue'
import UserInfo from './userInfo.vue'
import ResetPwd from './resetPwd.vue'

const activeTab = ref('info')
const profile = ref(null)

async function fetchProfile() { profile.value = await getProfile() }
onMounted(fetchProfile)
</script>
