<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
    <el-form-item label="昵称" prop="nickName"><el-input v-model="form.nickName" /></el-form-item>
    <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
    <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" /></el-form-item>
    <el-form-item label="性别"><el-select v-model="form.sex" style="width:120px"><el-option label="男" value="0" /><el-option label="女" value="1" /></el-select></el-form-item>
    <el-form-item><el-button type="primary" @click="submit">保存</el-button></el-form-item>
  </el-form>
</template>

<script setup>
import { reactive, watch, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateSelfSimple } from '../../../../api/system/user'

const props = defineProps({ profile: Object })
const emit = defineEmits(['updated'])
const formRef = ref()
const form = reactive({ nickName: '', phone: '', email: '', sex: '2' })
const rules = { nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }], phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }], email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }] }

watch(() => props.profile, (v) => { if (v) Object.assign(form, v) }, { immediate: true })

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    await updateSelfSimple(form)
    ElMessage.success('保存成功')
    emit('updated')
  })
}
</script>
