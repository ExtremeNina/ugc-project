package com.example.onlyone.DTO;

import lombok.Data;

@Data
public class LoveDTO {

    //实体id，可以为评论id也可以是文章id
    private Long id;
    private Long loveTypeId;
}
