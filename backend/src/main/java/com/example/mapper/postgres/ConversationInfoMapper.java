package com.example.mapper.postgres;

import com.example.entity.ConversationInfo;

import java.util.List;

public interface ConversationInfoMapper {


    List<ConversationInfo> selectByUserId(Long userId);

    ConversationInfo selectById(Long id);

    int insertConversationName(ConversationInfo cn);

    int updateConversationName(ConversationInfo cn);

    int deleteConversationName(Long id);
}
