<template>
  <div class="login-container">
    <div class="login-card">
      <div class="brand">
        <span class="dot" />
        <span class="brand-name">yiling manager</span>
      </div>
      <p class="subtitle">通用后台管理系统</p>

      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="passwd">
          <el-input v-model="form.passwd" type="password" placeholder="密码" size="large" show-password :prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button :loading="loading" type="primary" size="large" class="submit-btn" @click="handleLogin">登 录</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
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
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background:
    radial-gradient(900px circle at 15% 20%, rgba(91, 95, 239, 0.35), transparent 60%),
    radial-gradient(900px circle at 85% 80%, rgba(91, 95, 239, 0.2), transparent 60%),
    var(--y-canvas-dark);
}
.login-card {
  width: 380px;
  padding: 40px 36px 36px;
  background: var(--y-surface);
  border-radius: var(--y-radius-lg);
  box-shadow: var(--y-shadow-lg);
}
.brand { display: flex; align-items: center; gap: 10px; justify-content: center; }
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--y-accent);
  box-shadow: 0 0 0 5px rgba(91, 95, 239, 0.18);
}
.brand-name { font-size: 20px; font-weight: 700; letter-spacing: 0.2px; color: var(--y-text-primary); }
.subtitle { text-align: center; color: var(--y-text-secondary); font-size: 13px; margin: 6px 0 28px; }
.login-form :deep(.el-input__wrapper) { border-radius: var(--y-radius-sm); }
.submit-btn { width: 100%; letter-spacing: 2px; }
</style>
