package com.example.entity;
import java.util.Date;
public class User{

    private Long id;          // 用户ID
    private String username;     // 用户名
    private String email;        // 邮箱
    private String password;     // 密码
    private Integer status;      // 状态：1=激活，0=禁用
    private Date createdAt;      // 创建时间
    private Date updatedAt;      // 更新时间
    private String departmentName;
    private String roleName;
    private Integer departmentId;
    private Integer roleId;

    public User() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDepartmentName() { return departmentName;}

    public void setDepartmentName(String departmentName) {this.departmentName = departmentName;}

    public String getRoleName() { return roleName;}

    public void setRoleName(String roleName) {this.roleName = roleName;}

    public Integer getDepartmentId() { return departmentId;}

    public void setDepartmentId(Integer departmentId) {this.departmentId = departmentId;}

    public Integer getRoleId() {return roleId;}

    public void setRoleId(Integer roleId) {this.roleId = roleId;}

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
