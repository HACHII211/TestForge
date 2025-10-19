package com.example.auth;

import com.example.entity.User;
import com.example.security.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 使用 OrganizationMapper 作为 UserMapper 的替代（你项目中已存在的 mapper）。
 */
@Service
public class AuthService {

    @Resource
    private com.example.mapper.OrganizationMapper organizationMapper; // 你提供的 user mapper

    @Resource
    private com.example.mapper.UserRoleMapper userRoleMapper; // 可选：当 user 表没有 roleId 字段时回退使用

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 登录：校验用户名/密码 -> 读取 roleIds -> 生成 token
     *
     * 兼容说明：
     * - 如果 user 表里有 role_id（你的 OrganizationMapper resultMap 有 roleId），优先使用它作为单个 roleId。
     * - 如果没有则调用 userRoleMapper.selectRoleIdsByUserId(userId)（请确保这个 mapper 方法存在）。
     * - 密码校验优先用 BCrypt；若存储不为 BCrypt（没有 "$2a$" 或 "$2b$" 前缀），回退为明文比较（仅为迁移/调试方便）。
     */
    public AuthResponse login(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            throw new IllegalArgumentException("username and password required");
        }

        // 使用 selectByFilters 查用户（你的 OrganizationMapper 提供此方法）
        User filter = new User();
        filter.setUsername(username);
        List<User> users = organizationMapper.selectByFilters(filter);

        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("invalid credentials");
        }

        // 取第一个匹配的用户
        User user = users.get(0);
        if (user == null) {
            throw new IllegalArgumentException("invalid credentials");
        }

        String stored = user.getPassword();
        if (stored == null) {
            throw new IllegalArgumentException("invalid credentials");
        }

        boolean passwordMatches = false;
        // 优先 BCrypt 校验（生产中推荐统一使用 BCrypt 存储）
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            passwordMatches = passwordEncoder.matches(rawPassword, stored);
        } else {
            // 回退：直接字符串比对（仅用于兼容旧密码或测试环境）
            passwordMatches = Objects.equals(rawPassword, stored);
        }

        if (!passwordMatches) {
            throw new IllegalArgumentException("invalid credentials");
        }

        // 获取 roleIds：优先使用 user.roleId（单角色字段），否则尝试 user_role 关联表
        List<Long> roleIds = Collections.emptyList();
        try {
            // 假设你的 User.getRoleId() 返回 Long 或 Integer
            Number roleNum = null;
            try {
                // 反射式兼容：如果 getRoleId 返回 Integer/Long，直接拿到 Number
                Object obj = user.getRoleId(); // 若 User 类中是 Integer/Long，调整此处为正确 getter
                if (obj instanceof Number) roleNum = (Number) obj;
            } catch (NoSuchMethodError | Exception ignored) {
                // 如果 User 没有 roleId 字段或 getter 名称不同，忽略并回退到 userRoleMapper
            }

            if (roleNum != null) {
                roleIds = List.of(roleNum.longValue());
            } else if (userRoleMapper != null) {
                // 回退查 user_role 表（请确保你的 UserRoleMapper 有该方法）
                List<Long> fromJoin = userRoleMapper.selectRoleIdsByUserId(user.getId());
                roleIds = fromJoin == null ? Collections.emptyList() : fromJoin;
            } else {
                roleIds = Collections.emptyList();
            }
        } catch (Exception ex) {
            // 不要因为角色查不到而阻塞登录 —— 只是把 roleIds 设为空
            roleIds = Collections.emptyList();
        }

        // 生成 token（token 仅含 userId & roles）
        String token = jwtUtil.generateToken(user.getId(), roleIds);

        // 获取过期时间（毫秒）以便返回
        long expiresAt = jwtUtil.verifyToken(token).getExpiresAt().getTime();

        return new AuthResponse(token, expiresAt);
    }

    /** 用于在注册/手动设置密码时生成 bcrypt hash（可选） */
    public String encodePassword(String raw) {
        return passwordEncoder.encode(raw);
    }
}
