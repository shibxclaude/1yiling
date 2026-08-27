package com.yiling.modules.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class RouterVO {
    private String name;
    private String path;
    private String component;
    private Boolean hidden;
    private Meta meta;
    private List<RouterVO> children;

    @Data
    public static class Meta {
        private String title;
        private String icon;
        private Boolean noCache;

        public Meta(String title, String icon, Boolean noCache) {
            this.title = title;
            this.icon = icon;
            this.noCache = noCache;
        }
    }
}
