package com.yiling.modules.dept;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SysDeptControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void deptTree_containsSeededHeadOffice() throws Exception {
        mockMvc.perform(post("/rest/sysDept/deptTree").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].label").value("总公司"));
    }

    @Test
    @WithMockUser
    void delete_blockedWhenChildrenExist() throws Exception {
        Map<String, Object> child = Map.of("deptName", "子部门", "deptCode", "SUB1", "parentId", 100, "orderNum", 1, "status", 1);
        mockMvc.perform(post("/rest/sysDept/save").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(child)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/sysDept/delete").contentType(MediaType.APPLICATION_JSON).content("{\"id\":100}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("存在下级部门，不允许删除"));
    }
}
