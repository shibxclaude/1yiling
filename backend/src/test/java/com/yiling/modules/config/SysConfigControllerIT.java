package com.yiling.modules.config;

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
class SysConfigControllerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser
    void listPage_findsSeededSkinConfig() throws Exception {
        mockMvc.perform(post("/rest/sysConfig/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"configKey\":\"sys.index.skinName\",\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].configValue").value("skin-blue"));
    }

    @Test
    @WithMockUser
    void refreshCache_isNoOpSuccess() throws Exception {
        mockMvc.perform(post("/rest/sysConfig/refreshCache").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
