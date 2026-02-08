package com.example.onlyone.VO;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.socket.WebSocketMessage;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class WebsocketMessage {

    private String type;                    // 消息类型
    private Map<String, Object> data;      // 消息数据
    private Long timestamp;                //时间戳


}
