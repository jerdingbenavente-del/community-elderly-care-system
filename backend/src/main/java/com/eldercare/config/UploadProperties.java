package com.eldercare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "eldercare.upload")
public class UploadProperties {

    /** 相对或绝对本地目录，默认 uploads（相对进程工作目录） */
    private String baseDir = "uploads";
    private String avatarSubdir = "avatar";
    private long maxAvatarBytes = 2 * 1024 * 1024L;

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getAvatarSubdir() {
        return avatarSubdir;
    }

    public void setAvatarSubdir(String avatarSubdir) {
        this.avatarSubdir = avatarSubdir;
    }

    public long getMaxAvatarBytes() {
        return maxAvatarBytes;
    }

    public void setMaxAvatarBytes(long maxAvatarBytes) {
        this.maxAvatarBytes = maxAvatarBytes;
    }
}
