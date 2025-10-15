package com.example.service;


import com.example.entity.Permission;
import com.example.mapper.PermissionMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {

    @Resource
    private PermissionMapper permissionMapper;

    public PageInfo<Permission> selectByPage(Integer PageNum, Integer PageSize){
        List<Permission> list = permissionMapper.selectAll();
        PageHelper.startPage(PageNum, PageSize);
        return PageInfo.of(list);
    }
}
