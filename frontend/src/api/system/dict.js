import request from '../../utils/request'

export function listDictTypePage(query) { return request.post('/rest/sysDictType/listPage', query) }
export function getDictType(id) { return request.post('/rest/sysDictType/detailById', { id }) }
export function addDictType(data) { return request.post('/rest/sysDictType/save', data) }
export function updateDictType(data) { return request.post('/rest/sysDictType/update', data) }
export function delDictType(ids) { return request.post('/rest/sysDictType/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }

export function listDictDataPage(query) { return request.post('/rest/sysDictData/listPage', query) }
export function listDictDataByType(dictType) { return request.post('/rest/sysDictData/list', { dictType }) }
export function getDictData(id) { return request.post('/rest/sysDictData/detailById', { id }) }
export function addDictData(data) { return request.post('/rest/sysDictData/save', data) }
export function updateDictData(data) { return request.post('/rest/sysDictData/update', data) }
export function delDictData(ids) { return request.post('/rest/sysDictData/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }
