package com.example.mapper.postgres;

import com.example.entity.Conversation;
import com.example.entity.ConversationInfo;

import java.util.List;
import java.util.Map;

public interface ConversationMapper {

    List<Conversation> selectAll(Map<String, Object> params);

    Conversation selectById(Long id);

    Conversation selectByUuid(String conversationUuid);

    int insertConversation(Conversation conv);

    int updateConversation(Conversation conv);

    int deleteConversation(Long id);

    int deleteBatch(List<Long> ids);

    int appendMessageByUuid(Map<String, Object> params);

}
