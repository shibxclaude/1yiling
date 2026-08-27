import request from '../../utils/request'

export function listLogPage(query) { return request.post('/rest/sysOperLog/listPage', query) }
export function getLog(id) { return request.post('/rest/sysOperLog/detailById', { id }) }
export function delLog(ids) { return request.post('/rest/sysOperLog/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }
