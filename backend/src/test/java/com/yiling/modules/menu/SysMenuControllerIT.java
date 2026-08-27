package com.yiling.modules.menu;

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
class SysMenuControllerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser
    void roleMenuTreeSelect_adminHasAllMenusChecked() throws Exception {
        mockMvc.perform(post("/rest/sysMenu/roleMenuTreeSelect").contentType(MediaType.APPLICATION_JSON).content("{\"roleId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedKeys.length()").value(50));
    }

    @Test
    @WithMockUser
    void delete_blockedWhenChildMenuExists() throws Exception {
        mockMvc.perform(post("/rest/sysMenu/delete").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("存在子菜单，不允许删除"));
    }
}
