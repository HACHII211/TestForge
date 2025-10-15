package com.example.service;
import com.example.entity.User;
import com.example.mapper.OrganizationMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationService {
    @Resource
    public OrganizationMapper organizationMapper;


    public PageInfo<User> selectByPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> list = organizationMapper.selectAll();
        return PageInfo.of(list);
    }

    public void addUser(User user) {
        organizationMapper.addUser(user);
    }

    public void deleteUser(Integer id) {
        organizationMapper.deleteUser(id);
    }

    public void updateUser(User user) {
        organizationMapper.updateUser(user);
    }

    public PageInfo<User> selectByName(String username, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> list = organizationMapper.selectByName(username);
        return PageInfo.of(list);
    }
}
