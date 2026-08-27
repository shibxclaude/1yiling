import request from '../../utils/request'

export function listRolePage(query) { return request.post('/rest/sysRole/listPage', query) }
export function getRole(id) { return request.post('/rest/sysRole/detailById', { id }) }
export function addRole(data) { return request.post('/rest/sysRole/save', data) }
export function updateRole(data) { return request.post('/rest/sysRole/update', data) }
export function delRole(id) { return request.post('/rest/sysRole/delete', { id }) }
export function updateRoleStatus(id, status) { return request.post('/rest/sysRole/updateSimple', { id, status }) }
export function roleMenuTreeSelect(roleId) { return request.post('/rest/sysMenu/roleMenuTreeSelect', { roleId }) }
export function deptTreeByRole(roleId) { return request.post('/rest/dataPerm/deptTreeByRole', { roleId }) }
export function saveDataScope(data) { return request.post('/rest/dataPerm/dataScope', data) }
export function allocatedUserList(query) { return request.post('/rest/sysUserRole/allocatedList', query) }
export function unallocatedUserList(query) { return request.post('/rest/sysUserRole/unallocatedList', query) }
export function cancelUserRole(userId, roleId) { return request.post('/rest/sysUserRole/cancel', { userId, roleId }) }
export function cancelUserRoleAll(roleId, userIds) { return request.post('/rest/sysUserRole/cancelAll', { roleId, userIds }) }
export function selectUserRoleAll(roleId, userIds) { return request.post('/rest/sysUserRole/selectUserAll', { roleId, userIds }) }
