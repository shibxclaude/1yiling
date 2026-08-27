import request from '../../utils/request'

export function listUserPage(query) { return request.post('/rest/sysUser/listPage', query) }
export function getUser(id) { return request.post('/rest/sysUser/detailById', { id }) }
export function addUser(data) { return request.post('/rest/sysUser/save', data) }
export function updateUser(data) { return request.post('/rest/sysUser/update', data) }
export function delUser(id) { return request.post('/rest/sysUser/delete', { id }) }
export function updateUserStatus(id, status) { return request.post('/rest/sysUser/updateSimple', { id, status }) }
export function resetUserPwd(id, passwd) { return request.post('/rest/sysUser/resetPwd', { id, passwd }) }
export function getProfile() { return request.post('/rest/sysUser/profile', {}) }
export function updateSelfSimple(data) { return request.post('/rest/sysUser/updateSelfSimple', data) }
export function updateSelfPwd(data) { return request.post('/rest/sysUser/updatePwd', data) }
export function uploadAvatar(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/rest/sysUser/avatar', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}
