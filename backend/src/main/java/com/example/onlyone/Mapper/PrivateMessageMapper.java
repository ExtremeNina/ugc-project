package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.PrivateMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    // [实时通信升级] 重试幂等：同一发送者的 clientMessageId 只对应一条服务端消息
    @Select("SELECT * FROM private_message WHERE user_id = #{senderId} AND client_message_id = #{clientMessageId} LIMIT 1")
    PrivateMessage selectByClientMessageId(@Param("senderId") Long senderId,
                                           @Param("clientMessageId") String clientMessageId);

    /**
     * 批量查询"我与每个好友的会话各自最新一条消息"（窗口函数，一条 SQL 替代 N 次 getLastText）
     * SQL 见 PrivateMessgaeMapper.xml
     * 说明：这是全项目唯一保留的自定义 SQL——窗口函数 row_number() 无法用 MP lambda 表达，
     * 其余简单查询（已读更新、未读计数、聊天记录分页）均已改为 Service 层的 LambdaQueryWrapper/UpdateWrapper
     */
    List<PrivateMessage> selectLastMessagePerPeer(@Param("userId") Long userId,
                                                  @Param("peerIds") List<Long> peerIds);
}
