package com.thabo.howsouthaareyou.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String dir = "/app/uploads";
    private String urlPrefix = "/uploads";

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        if (dir != null && !dir.isBlank()) {
            this.dir = dir;
        }
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        if (urlPrefix != null && !urlPrefix.isBlank()) {
            this.urlPrefix = urlPrefix;
        }
    }
}