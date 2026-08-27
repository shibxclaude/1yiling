import request from '../../utils/request'

export function listNoticePage(query) { return request.post('/rest/sysNotice/listPage', query) }
export function getNotice(id) { return request.post('/rest/sysNotice/detailById', { id }) }
export function addNotice(data) { return request.post('/rest/sysNotice/save', data) }
export function updateNotice(data) { return request.post('/rest/sysNotice/update', data) }
export function delNotice(ids) { return request.post('/rest/sysNotice/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }
