<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
    <el-form-item label="旧密码" prop="oldPassword"><el-input v-model="form.oldPassword" type="password" show-password /></el-form-item>
    <el-form-item label="新密码" prop="newPassword"><el-input v-model="form.newPassword" type="password" show-password /></el-form-item>
    <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password /></el-form-item>
    <el-form-item><el-button type="primary" @click="submit">保存</el-button></el-form-item>
  </el-form>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateSelfPwd } from '../../../../api/system/user'

const formRef = ref()
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 5, max: 20, message: '密码长度必须在5-20位之间', trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: (r, v, cb) => (v !== form.newPassword ? cb(new Error('两次输入的密码不一致')) : cb()), trigger: 'blur' }]
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    await updateSelfPwd({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码修改成功')
    form.oldPassword = ''; form.newPassword = ''; form.confirmPassword = ''
  })
}
</script>
