<template>
  <div class="login-container">
    <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
      <h3 class="title">yiling-manager 后台管理系统</h3>
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名" size="large" />
      </el-form-item>
      <el-form-item prop="passwd">
        <el-input v-model="form.passwd" type="password" placeholder="密码" size="large" show-password @keyup.enter="handleLogin" />
      </el-form-item>
      <el-button :loading="loading" type="primary" size="large" style="width:100%" @click="handleLogin">登录</el-button>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: 'admin', passwd: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  passwd: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function handleLogin() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(form.username, form.passwd)
      await userStore.getInfo()
      router.push('/')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container { display:flex; align-items:center; justify-content:center; height:100vh; background:#2d3a4b; }
.login-form { width:400px; padding:35px; background:#fff; border-radius:6px; }
.title { text-align:center; margin-bottom:24px; }
</style>
