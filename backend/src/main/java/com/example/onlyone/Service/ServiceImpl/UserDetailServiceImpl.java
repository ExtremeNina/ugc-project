package com.example.onlyone.Service.ServiceImpl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.onlyone.Entity.*;
import com.example.onlyone.Mapper.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户详情加载服务（Spring Security UserDetailsService 实现）
 *
 * RBAC 权限加载链：
 * User → UserAndRole → Role → RoleAndPermission → Permission
 *
 * 缓存优化说明：
 * 优化前每次请求都走完整的 5 表联查链（用户有 2 个角色 = 7+ 次 DB 查询），
 * 优化后引入 Redis 缓存，首次加载后将权限列表（仅 authority 字符串）缓存 30 分钟，
 * 后续请求直接从缓存反序列化，将 7+ 次 DB 查询降为 1 次 Redis GET。
 *
 * 缓存 Key 格式：user:authorities:{userId}
 * 缓存 Value 格式：JSON 字符串数组，如 ["ROLE_USER","article:read"]
 */
@Service
@Slf4j
public class UserDetailServiceImpl implements UserDetailsService {

    private static final String AUTHORITIES_CACHE_PREFIX = "user:authorities:";
    private static final long AUTHORITIES_CACHE_TTL_MINUTES = 30;

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserAndRoleMapper userAndRoleMapper;
    @Resource
    private RoleAndPermissionMapper roleAndPermissionMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PermissionMapper permissionMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 根据用户名加载用户详情及权限
     *
     * 流程：
     * 1. 查 User 表获取用户基本信息（不缓存，保证用户状态实时性）
     * 2. 从 Redis 缓存中获取权限列表
     * 3. 缓存未命中时走 DB 完整 RBAC 加载链 → 写缓存
     */
    @Override
    public UserDetail loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.error("用户名不存在: {}", username);
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        List<GrantedAuthority> authorities = getCachedAuthorities(user.getId());
        log.info("用户：{}的角色和权限:{}", username, authorities);

        return new UserDetail(user, passwordEncoder, authorities);
    }

    /**
     * 获取权限列表（缓存优先）
     *
     * 缓存命中：直接 JSON 反序列化为 List<SimpleGrantedAuthority>
     * 缓存未命中：走 DB 加载链 → 序列化为 JSON 写入 Redis（TTL 30 分钟）
     */
    private List<GrantedAuthority> getCachedAuthorities(Long userId) {
        String key = AUTHORITIES_CACHE_PREFIX + userId;
        String cached = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(cached)) {
            List<String> authorityNames = JSONUtil.toList(cached, String.class);
            log.info("从缓存加载用户 {} 的权限，共 {} 条", userId, authorityNames.size());
            return authorityNames.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        }

        List<GrantedAuthority> authorities = loadAuthoritiesFromDB(userId);
        List<String> authorityNames = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(authorityNames),
                AUTHORITIES_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("从数据库加载用户 {} 的权限并写入缓存，共 {} 条", userId, authorityNames.size());

        return authorities;
    }

    /**
     * 从数据库加载完整的 RBAC 权限链
     *
     * 查询链路：
     * user_and_role (按 userId) → role (按 roleIds) → role_and_permission (按 roleId) → permission (按 permissionIds)
     *
     * 权限格式：
     * - 角色：ROLE_{ROLENAME}（如 ROLE_USER，自动去除重复的 ROLE_ 前缀）
     * - 权限：直接使用 permission.name（如 article:write）
     */
    private List<GrantedAuthority> loadAuthoritiesFromDB(Long userId) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        List<UserAndRole> userAndRoles = userAndRoleMapper.queryRoles(userId);
        if (userAndRoles == null || userAndRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userAndRoles.stream().map(UserAndRole::getRoleId).toList();
        List<Role> roles = roleMapper.selectAllRoles(roleIds);

        for (Role role : roles) {
            // 添加角色权限（Spring Security 格式：ROLE_XXX）
            // replace("ROLE_", "") 防止角色名已经带 ROLE_ 前缀时重复拼接
            authorities.add(new SimpleGrantedAuthority(
                    "ROLE_" + role.getRoleName().toUpperCase().replace("ROLE_", "")));

            List<RoleAndPermission> roleAndPermissions = roleAndPermissionMapper.queryPermission(role.getId());
            if (roleAndPermissions == null || roleAndPermissions.isEmpty()) {
                continue;
            }

            List<Long> permissionIds = roleAndPermissions.stream()
                    .map(RoleAndPermission::getPermissionId).toList();
            List<Permission> permissions = permissionMapper.selectAllPermission(permissionIds);
            for (Permission permission : permissions) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }

        return authorities;
    }

    /**
     * 清除用户权限缓存
     *
     * 使用场景：管理员修改用户角色/权限后调用，下次请求自动从 DB 重建缓存。
     */
    public void evictAuthoritiesCache(Long userId) {
        String key = AUTHORITIES_CACHE_PREFIX + userId;
        stringRedisTemplate.delete(key);
        log.info("已清除用户 {} 的权限缓存", userId);
    }
}
