import request from '../../utils/request'

export function listPostPage(query) { return request.post('/rest/sysPost/listPage', query) }
export function listPost(query) { return request.post('/rest/sysPost/list', query || {}) }
export function getPost(id) { return request.post('/rest/sysPost/detailById', { id }) }
export function addPost(data) { return request.post('/rest/sysPost/save', data) }
export function updatePost(data) { return request.post('/rest/sysPost/update', data) }
export function delPost(ids) { return request.post('/rest/sysPost/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }
export function updatePostStatus(id, status) { return request.post('/rest/sysPost/updateSimple', { id, status }) }
