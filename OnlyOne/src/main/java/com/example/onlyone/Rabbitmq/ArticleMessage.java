package com.example.onlyone.Rabbitmq;

import lombok.Data;

@Data
public class ArticleMessage {

    private Long messageId;
    private Long articleId;
    private Long authorId;
}
