import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({ size: 'default' })
})
