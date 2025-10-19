// PermissionAspect.java
package com.example.security;

import com.example.exception.AccessDeniedRuntimeException;
import com.example.mapper.PermissionMapper;
import com.example.mapper.RolePermissionMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class PermissionAspect {

    @Resource
    private RolePermissionMapper rolePermissionMapper; // 假定存在的 Mapper
    @Resource
    private PermissionMapper permissionMapper;         // 假定存在的 Mapper

    @Pointcut("@annotation(com.example.security.RequiresPermission)")
    public void permissionPointcut() {}

    @Before("permissionPointcut() && @annotation(reqPerm)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermission reqPerm) {
        String required = reqPerm.value();

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new AccessDeniedRuntimeException("No request context");
        }
        HttpServletRequest request = attrs.getRequest();

        @SuppressWarnings("unchecked")
        List<Long> roleIds = (List<Long>) request.getAttribute(AuthInterceptor.ATTR_ROLE_IDS);
        if (roleIds == null || roleIds.isEmpty()) {
            throw new AccessDeniedRuntimeException("No roles found, access denied");
        }

        // 1) 通过 roleIds 查 permissionIds（去重）
        List<Long> permIds = rolePermissionMapper.selectPermissionIdsByRoleIds(roleIds);
        if (permIds == null || permIds.isEmpty()) {
            throw new AccessDeniedRuntimeException("No permissions assigned to roles");
        }

        // 2) 通过 permissionIds 查 permission name
        List<String> permNames = permissionMapper.selectNamesByIds(permIds);

        Set<String> permNameSet = permNames.stream().collect(Collectors.toSet());
        if (!permNameSet.contains(required)) {
            throw new AccessDeniedRuntimeException("Permission denied: " + required);
        }
        // else 放行
    }
}
