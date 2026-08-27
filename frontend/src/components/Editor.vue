<template>
  <div style="border:1px solid #ccc;">
    <Toolbar :editor="editorRef" :default-config="toolbarConfig" style="border-bottom:1px solid #ccc;" />
    <Editor v-model="content" :default-config="editorConfig" style="height:300px; overflow-y:auto;" @on-created="(e) => (editorRef = e)" />
  </div>
</template>

<script setup>
import { ref, shallowRef, watch, onBeforeUnmount } from 'vue'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef()
const content = ref(props.modelValue)
const toolbarConfig = {}
const editorConfig = { placeholder: '请输入内容...' }

watch(() => props.modelValue, (v) => { if (v !== content.value) content.value = v })
watch(content, (v) => emit('update:modelValue', v))

onBeforeUnmount(() => {
  if (editorRef.value) editorRef.value.destroy()
})
</script>
