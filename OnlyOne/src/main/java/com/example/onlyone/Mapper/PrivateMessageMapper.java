package com.example.onlyone.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.PrivateMessage;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {
    List<PrivateMessage> selectByCurrent(Long userId,Long id);

    PrivateMessage getLastText(Long userId, Long id);


    @Update("update private_message set status = 1 where receive_id = #{userId} and user_id = #{id} and status = 0")
    void updateStatus(Long userId,Long id);


    @Select("select count(*) from private_message where receive_id = #{userId} and status = 0")
    Long selectUnreadCount(Long userId);
}
