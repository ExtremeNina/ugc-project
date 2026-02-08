package com.example.onlyone.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleAndPermission {

    private Long id;
    private Long roleId;
    private Long permissionId;
    private LocalDateTime createTime;


}
