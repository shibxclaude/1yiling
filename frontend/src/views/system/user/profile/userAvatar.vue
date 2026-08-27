<template>
  <div>
    <img :src="userStore.avatar || defaultAvatar" style="width:100px;height:100px;border-radius:50%;object-fit:cover;cursor:pointer;" @click="fileInput.click()" />
    <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />
    <el-dialog v-model="cropVisible" title="裁剪头像" width="500px">
      <img ref="cropImg" :src="rawImage" style="max-width:100%;display:block;" />
      <template #footer><el-button @click="cropVisible = false">取消</el-button><el-button type="primary" @click="confirmCrop">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'
import { ElMessage } from 'element-plus'
import { uploadAvatar } from '../../../../api/system/user'
import { useUserStore } from '../../../../stores/user'

const userStore = useUserStore()
const fileInput = ref()
const cropImg = ref()
const cropVisible = ref(false)
const rawImage = ref('')
const defaultAvatar = '/uploads/avatar/default.png'
let cropper = null

function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  rawImage.value = URL.createObjectURL(file)
  cropVisible.value = true
  nextTick(() => {
    if (cropper) cropper.destroy()
    cropper = new Cropper(cropImg.value, { aspectRatio: 1, viewMode: 1 })
  })
}

function confirmCrop() {
  cropper.getCroppedCanvas().toBlob(async (blob) => {
    const file = new File([blob], 'avatar.png', { type: 'image/png' })
    const data = await uploadAvatar(file)
    userStore.avatar = data.url
    ElMessage.success('头像更新成功')
    cropVisible.value = false
  })
}
</script>
