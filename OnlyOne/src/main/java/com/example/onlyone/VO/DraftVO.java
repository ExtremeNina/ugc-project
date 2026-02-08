package com.example.onlyone.VO;

import lombok.Data;

import java.util.List;

@Data
public class DraftVO {

    private String title;
    private Long author_id;
    private Long category_id;
    private int status;
    private List<Long> label;
    private String content;
    private String summary;
    private String coverImageUrl;

}
