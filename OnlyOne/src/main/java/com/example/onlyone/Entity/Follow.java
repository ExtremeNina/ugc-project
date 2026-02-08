package com.example.onlyone.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Follow {

    @TableId(type = IdType.AUTO)
    private Long id;
    //关注者id也就说当前用户id
    private Long followerId;
    //关注者id
    private Long followingId;
    private LocalDateTime createTime;

}
