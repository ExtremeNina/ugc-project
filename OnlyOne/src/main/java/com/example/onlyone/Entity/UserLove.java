package com.example.onlyone.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLove {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long entityId;
    private LocalDateTime createTime;
    private Long loveTypeId;

}
