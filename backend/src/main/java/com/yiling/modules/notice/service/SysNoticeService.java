package com.yiling.modules.notice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.notice.dto.SysNoticeDTO;
import com.yiling.modules.notice.entity.SysNotice;

import java.util.List;

public interface SysNoticeService extends IService<SysNotice> {
    PageResult<SysNotice> listPage(SysNoticeDTO query);
    SysNotice detailById(Long id);
    void save(SysNoticeDTO dto);
    void update(SysNoticeDTO dto);
    void delete(List<Long> ids);
}
