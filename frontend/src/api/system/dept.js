import request from '../../utils/request'

export function listDept(query) { return request.post('/rest/sysDept/list', query) }
export function deptTree() { return request.post('/rest/sysDept/deptTree', {}) }
export function getDept(id) { return request.post('/rest/sysDept/detailById', { id }) }
export function addDept(data) { return request.post('/rest/sysDept/save', data) }
export function updateDept(data) { return request.post('/rest/sysDept/update', data) }
export function delDept(id) { return request.post('/rest/sysDept/delete', { id }) }
