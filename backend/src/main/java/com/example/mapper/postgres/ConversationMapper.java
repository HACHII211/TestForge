package com.example.mapper.postgres;

import com.example.entity.Conversation;
import java.util.List;
import java.util.Map;

public interface ConversationMapper {

    /**
     * 分页 / 条件查询（params 可包含: userId, assistantId, dateFrom, dateTo, q）
     * PageHelper 在 Service 中启动分页
     */
    List<Conversation> selectAll(Map<String, Object> params);

    Conversation selectById(Long id);

    Conversation selectByUuid(String conversationUuid);

    int insertConversation(Conversation conv);

    int updateConversation(Conversation conv);

    int deleteConversation(Long id);

    int deleteBatch(List<Long> ids);

    /**
     * 原子追加一条消息（messageJson 必须是 JSON 字符串）
     */
    int appendMessageByUuid(Map<String, Object> params);
}
