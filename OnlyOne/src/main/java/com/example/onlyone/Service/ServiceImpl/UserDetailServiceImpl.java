package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.onlyone.Entity.*;
import com.example.onlyone.Mapper.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UserDetailServiceImpl implements UserDetailsService {

      @Resource
      private UserMapper userMapper;
      @Resource
      private PasswordEncoder passwordEncoder;
      @Resource
      private UserAndRoleMapper userAndRoleMapper;
      @Resource
      private RoleAndPermissionMapper roleAndPermissionMapper;
      @Resource
      private RoleMapper roleMapper;
      @Resource
      private PermissionMapper permissionMapper;

      @Override
      public UserDetail loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.error("用户名不存在: {}", username);
            throw new UsernameNotFoundException("用户名或密码错误");
        }

      List<GrantedAuthority> authorities = new ArrayList<>();
      //查询用户所拥有的角色
      List<UserAndRole> userAndRoles = userAndRoleMapper.queryRoles(user.getId());
      //获取用户角色表中的role_id
      List<Long> roleIds = userAndRoles.stream().map(UserAndRole::getRoleId).toList();
      //根据rol_id获取对应的角色数据
      List<Role> roles = roleMapper.selectAllRoles(roleIds);
      log.info(roles.toString());
      for (Role role : roles) {
          //转换出标准格式
          log.info("包含的角色"+role.getRoleName());
          authorities.add(new SimpleGrantedAuthority("ROLE_"+role.getRoleName().toUpperCase().replace("ROLE_", "")));
          //获取角色权限表中的数据
          List<RoleAndPermission> roleAndPermissions = roleAndPermissionMapper.queryPermission(role.getId());
          //获取角色权限表中的全部权限权限id
          List<Long> permissionIds = roleAndPermissions.stream().map(RoleAndPermission::getPermissionId).toList();
          //查询获取的全部权限
          List<Permission> permissions = permissionMapper.selectAllPermission(permissionIds);
          for (Permission permission : permissions) {
              authorities.add((new SimpleGrantedAuthority(permission.getName())));
          }
          log.info("用户：{}的角和色权限:{}",username, authorities);
      }

        return new UserDetail(user,passwordEncoder,authorities);
    }
}
