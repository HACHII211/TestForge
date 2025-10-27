package com.example.auth;

import com.example.entity.User;
import com.example.mapper.mysql.OrganizationMapper;
import com.example.mapper.mysql.PermissionMapper;
import com.example.mapper.mysql.RolePermissionMapper;
import com.example.mapper.mysql.UserRoleMapper;
import com.example.security.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Resource
    private OrganizationMapper organizationMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Resource
    private PermissionMapper permissionMapper;

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 登录并返回 token + expiresAt + permissions + userDTO
     */
    public AuthResponse login(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            throw new IllegalArgumentException("username and password required");
        }

        // 查询用户（使用你的 OrganizationMapper.selectByFilters）
        User filter = new User();
        filter.setUsername(username);
        List<User> users = organizationMapper.selectByFilters(filter);
        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("invalid credentials");
        }
        User user = users.get(0);
        if (user == null) throw new IllegalArgumentException("invalid credentials");

        String stored = user.getPassword();
        if (stored == null) throw new IllegalArgumentException("invalid credentials");

        boolean passwordMatches;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            passwordMatches = passwordEncoder.matches(rawPassword, stored);
        } else {
            // 回退明文比对（仅用于兼容/调试，生产请统一为 BCrypt）
            passwordMatches = Objects.equals(rawPassword, stored);
        }
        if (!passwordMatches) {
            throw new IllegalArgumentException("invalid credentials");
        }

        // --- 把 user 映射为 UserDTO（不暴露 password） ---
        UserDTO userDto = mapToDto(user);

        List<Long> roleIds = Collections.emptyList();
        try {
            Long singleRoleId = null;
            try {
                Object roleObj = user.getRoleId();
                if (roleObj instanceof Number) {
                    singleRoleId = ((Number) roleObj).longValue();
                }
            } catch (Throwable ignored) {
            }

            if (singleRoleId != null) {
                roleIds = List.of(singleRoleId);
            } else if (userRoleMapper != null) {
                List<Long> fromJoin = userRoleMapper.selectRoleIdsByUserId(user.getId());
                roleIds = fromJoin == null ? Collections.emptyList() : fromJoin;
            } else {
                roleIds = Collections.emptyList();
            }
        } catch (Exception ex) {
            roleIds = Collections.emptyList();
        }

        // 生成 token（只包含 userId 和 roles）
        String token = jwtUtil.generateToken(user.getId(), roleIds);
        long expiresAt = jwtUtil.verifyToken(token).getExpiresAt().getTime();

        // --- 合并权限 names（去重并规范为小写） ---
        List<String> permissionNames = Collections.emptyList();
        try {
            if (roleIds != null && !roleIds.isEmpty()) {
                // 1) 通过角色查 permission ids（去重）
                List<Long> permIds = rolePermissionMapper.selectPermissionIdsByRoleIds(roleIds);
                if (permIds != null && !permIds.isEmpty()) {
                    // 2) 通过 permission ids 查 names
                    List<String> names = permissionMapper.selectNamesByIds(permIds);
                    if (names != null && !names.isEmpty()) {
                        permissionNames = names.stream()
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .map(String::toLowerCase) // 统一小写，方便前端比较
                                .distinct()
                                .collect(Collectors.toList());
                    }
                }
            } else {
                permissionNames = Collections.emptyList();
            }
        } catch (Exception ex) {
            // 如果权限查不到，仍然允许登录（只是返回空权限列表）
            permissionNames = Collections.emptyList();
        }

        return new AuthResponse(token, expiresAt, permissionNames, userDto);
    }

    private UserDTO mapToDto(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        // 假定 User 实体提供了这些 getter 名称（与 organizationMapper 的 resultMap 对应）
        try {
            dto.setId(user.getId());
        } catch (Throwable ignored) {}
        try { dto.setUsername(user.getUsername()); } catch (Throwable ignored) {}
        try { dto.setEmail(user.getEmail()); } catch (Throwable ignored) {}
        try { dto.setStatus(user.getStatus()); } catch (Throwable ignored) {}
        try {
            Object dep = user.getDepartmentId();
            if (dep instanceof Number) dto.setDepartmentId(((Number) dep).longValue());
        } catch (Throwable ignored) {}
        try {
            Object rid = user.getRoleId();
            if (rid instanceof Number) dto.setRoleId(((Number) rid).longValue());
        } catch (Throwable ignored) {}
        try { dto.setDepartmentName(user.getDepartmentName()); } catch (Throwable ignored) {}
        try { dto.setRoleName(user.getRoleName()); } catch (Throwable ignored) {}
        try { dto.setCreatedAt(user.getCreatedAt()); } catch (Throwable ignored) {}
        try { dto.setUpdatedAt(user.getUpdatedAt()); } catch (Throwable ignored) {}
        return dto;
    }

    /** 生成 bcrypt hash（可用于注册/迁移） */
    public String encodePassword(String raw) {
        return passwordEncoder.encode(raw);
    }
}
