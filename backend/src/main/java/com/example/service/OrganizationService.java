package com.example.service;

import com.example.entity.User;
import com.example.entity.UserRole;
import com.example.mapper.mysql.OrganizationMapper;
import com.example.mapper.mysql.UserRoleMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationService {
    @Resource
    private OrganizationMapper organizationMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    public PageInfo<User> selectByPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> list = organizationMapper.selectAll();
        return PageInfo.of(list);
    }

    // 向后兼容：仅插入 user，不处理角色关系
    public void addUser(User user) {
        organizationMapper.addUser(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createUserWithRole(User user) {
        organizationMapper.addUser(user);
        Integer roleId = user.getRoleId();
        Long userId = user.getId();
        if (userId == null) {
            throw new IllegalStateException("插入 user 未返回 id，请确认 mapper 配置 useGeneratedKeys/selectKey 是否正确");
        }

        if (roleId != null) {
            UserRole mapping = new UserRole(userId, roleId);
            userRoleMapper.insertUserRole(mapping);
        }
    }

    public void deleteUser(Integer id) {
        organizationMapper.deleteUser(id);
    }

    public void updateUser(User user) {
        organizationMapper.updateUser(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserWithRole(User user) {
        // 更新 user 信息
        organizationMapper.updateUser(user);

        Long userId = user.getId();
        Integer roleId = user.getRoleId();
        if (userId == null) {
            throw new IllegalStateException("user id 为空，无法更新关联表");
        }

        // 删除旧关系（如果存在）
        userRoleMapper.deleteByUserId(userId);

        // 插入新的关系（如果 roleId 不为空）
        if (roleId != null) {
            userRoleMapper.insertUserRole(new UserRole(userId, roleId));
        }
    }

    public PageInfo<User> selectByFilters(User user, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> list = organizationMapper.selectByFilters(user);
        return PageInfo.of(list);
    }
}
