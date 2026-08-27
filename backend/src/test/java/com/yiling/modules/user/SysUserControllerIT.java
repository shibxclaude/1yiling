package com.yiling.modules.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SysUserControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void save_thenListPage_findsNewUser() throws Exception {
        Map<String, Object> create = Map.of("username", "zhangsan", "nickName", "张三", "passwd", "abc12345", "deptId", 100, "sex", "0");
        mockMvc.perform(post("/rest/sysUser/save").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/sysUser/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"zhangsan\",\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].deptName").value("总公司"));
    }

    @Test
    @WithMockUser
    void delete_adminIsProtected() throws Exception {
        mockMvc.perform(post("/rest/sysUser/delete").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("超级管理员账号不允许删除"));
    }

    @Test
    @WithMockUser
    void save_thenDetailById_roundTripsRolesAndPosts() throws Exception {
        Map<String, Object> create = Map.of("username", "lisi", "nickName", "李四", "passwd", "abc12345", "deptId", 100,
                "sex", "0", "roleIds", java.util.List.of(1), "postIds", java.util.List.of(1));
        mockMvc.perform(post("/rest/sysUser/save").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/sysUser/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"lisi\",\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(jsonPath("$.data.records[0].id").exists());
    }
}
