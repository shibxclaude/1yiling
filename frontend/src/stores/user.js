import { defineStore } from 'pinia'
import request from '../utils/request'
import { getToken, setToken, removeToken } from '../utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    name: '',
    nickName: '',
    avatar: '',
    roles: [],
    permissions: []
  }),
  actions: {
    async login(username, passwd) {
      const data = await request.post('/login', { username, passwd })
      this.token = data.token
      setToken(data.token)
    },
    async getInfo() {
      const data = await request.get('/getInfo')
      this.name = data.user.username
      this.nickName = data.user.nickName
      this.avatar = data.user.avatar
      this.roles = data.roles
      this.permissions = data.permissions
      return data
    },
    logout() {
      this.token = ''
      this.roles = []
      this.permissions = []
      removeToken()
    }
  }
})
