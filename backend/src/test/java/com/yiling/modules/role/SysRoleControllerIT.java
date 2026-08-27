package com.yiling.modules.role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SysRoleControllerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser
    void delete_adminRoleIsProtected() throws Exception {
        mockMvc.perform(post("/rest/sysRole/delete").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("超级管理员角色不允许删除"));
    }

    @Test
    @WithMockUser
    void allocatedList_returnsSeededAdminUser() throws Exception {
        mockMvc.perform(post("/rest/sysUserRole/allocatedList").contentType(MediaType.APPLICATION_JSON).content("{\"roleId\":1,\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }

    @Test
    @WithMockUser
    void dataScope_customThenBackToAll_clearsDeptIds() throws Exception {
        mockMvc.perform(post("/rest/dataPerm/dataScope").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"dataScope\":\"2\",\"deptIdList\":[100]}"))
                .andExpect(status().isOk());

        var deptTreeResp = mockMvc.perform(post("/rest/dataPerm/deptTreeByRole").contentType(MediaType.APPLICATION_JSON).content("{\"roleId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedKeys[0]").value(100))
                .andReturn();

        mockMvc.perform(post("/rest/dataPerm/dataScope").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"dataScope\":\"1\",\"deptIdList\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/dataPerm/deptTreeByRole").contentType(MediaType.APPLICATION_JSON).content("{\"roleId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedKeys.length()").value(0));
    }
}
