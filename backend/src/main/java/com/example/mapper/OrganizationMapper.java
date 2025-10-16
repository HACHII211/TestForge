package com.example.mapper;

import com.example.entity.User;

import java.util.List;

public interface OrganizationMapper {
    List<User> selectAll();

    void addUser(User user);

    void deleteUser(Integer id);

    void updateUser(User user);

    List<User> selectByFilters(User user);
}
