package com.eldercare.vo;

/**
 * 业务开户结果：不返回明文密码（初始密码固定为 123456，由管理端提示）。
 */
public class BusinessAccountVO {

    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private Boolean mustChangePassword;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
}
