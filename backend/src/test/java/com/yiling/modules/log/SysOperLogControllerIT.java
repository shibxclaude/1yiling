package com.yiling.modules.log;

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
class SysOperLogControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin")
    void savingAPost_automaticallyWritesOperLog() throws Exception {
        Map<String, Object> create = Map.of("postCode", "logtest1", "postName", "日志测试岗位", "postSort", 1);
        mockMvc.perform(post("/rest/sysPost/save").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/sysOperLog/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"SysPost\",\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].businessType").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value(0))
                .andExpect(jsonPath("$.data.records[0].operName").value("admin"));
    }

    @Test
    @WithMockUser(username = "admin")
    void deletingAPost_isCapturedWithBusinessType3() throws Exception {
        Map<String, Object> create = Map.of("postCode", "logtest2", "postName", "日志删除测试岗位", "postSort", 2);
        String body = mockMvc.perform(post("/rest/sysPost/save").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // find the id we just created via list, then delete it
        String listBody = mockMvc.perform(post("/rest/sysPost/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"postCode\":\"logtest2\",\"pageNo\":1,\"pageSize\":10}"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(listBody).get("data").get("records").get(0).get("id").asLong();

        mockMvc.perform(post("/rest/sysPost/delete").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + id + "]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/sysOperLog/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"SysPost\",\"businessType\":3,\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(username = "admin")
    void delete_asAdmin_succeeds() throws Exception {
        mockMvc.perform(post("/rest/sysOperLog/delete").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[999999]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "nobody-not-in-db")
    void delete_asNonAdmin_isRejected() throws Exception {
        mockMvc.perform(post("/rest/sysOperLog/delete").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[999999]}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("仅超级管理员可删除操作日志"));
    }
}
