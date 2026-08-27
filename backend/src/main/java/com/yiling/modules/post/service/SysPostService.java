package com.yiling.modules.post.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.post.dto.SysPostDTO;
import com.yiling.modules.post.entity.SysPost;

import java.util.List;

public interface SysPostService extends IService<SysPost> {
    PageResult<SysPost> listPage(SysPostDTO query);
    List<SysPost> list(SysPostDTO query);
    SysPost detailById(Long id);
    void save(SysPostDTO dto);
    void update(SysPostDTO dto);
    void delete(List<Long> ids);
    void updateSimple(Long id, Integer status);
}
