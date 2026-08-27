import request from '../../utils/request'

export function listConfigPage(query) { return request.post('/rest/sysConfig/listPage', query) }
export function getConfig(id) { return request.post('/rest/sysConfig/detailById', { id }) }
export function addConfig(data) { return request.post('/rest/sysConfig/save', data) }
export function updateConfig(data) { return request.post('/rest/sysConfig/update', data) }
export function delConfig(ids) { return request.post('/rest/sysConfig/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }
export function refreshConfigCache() { return request.post('/rest/sysConfig/refreshCache', {}) }
