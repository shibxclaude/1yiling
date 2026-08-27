import request from '../../utils/request'

export function listMenu(query) { return request.post('/rest/sysMenu/list', query || {}) }
export function menuTreeSelect() { return request.post('/rest/sysMenu/treeSelect', {}) }
export function getMenu(id) { return request.post('/rest/sysMenu/detailById', { id }) }
export function addMenu(data) { return request.post('/rest/sysMenu/save', data) }
export function updateMenu(data) { return request.post('/rest/sysMenu/update', data) }
export function delMenu(id) { return request.post('/rest/sysMenu/delete', { id }) }
