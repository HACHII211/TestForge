package com.example.service;

import com.example.entity.Conversation;
import com.example.entity.ConversationInfo;
import com.example.mapper.postgres.ConversationInfoMapper;
import com.example.mapper.postgres.ConversationMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ConversationService {

    @Resource
    private ConversationMapper mapper;

    @Resource
    private ConversationInfoMapper conversationInfoMapper;

    public PageInfo<Conversation> selectByPage(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Conversation> list = mapper.selectAll(params == null ? new HashMap<>() : params);
        return PageInfo.of(list);
    }

    public Conversation selectById(Long id) {
        return mapper.selectById(id);
    }

    public Conversation selectByUuid(String uuid) {
        return mapper.selectByUuid(uuid);
    }

    @Transactional
    public void addConversation(Conversation conv) {
        if (conv.getMessages() == null || conv.getMessages().trim().isEmpty()) {
            conv.setMessages("[]");
        }
        if (conv.getMetadata() == null || conv.getMetadata().trim().isEmpty()) {
            conv.setMetadata("{}");
        }

        // 1️⃣ 插入 conversations
        mapper.insertConversation(conv);

        System.out.println("new uuid = " + conv.getConversationUuid());

        // 2️⃣ 插入 conversation_name
        ConversationInfo info = new ConversationInfo();
        info.setConversationUuid(conv.getConversationUuid());
        info.setUserId(conv.getUserId());
        info.setName("新对话");
        info.setIsPrimary(false);
        info.setVisibility("private");

        conversationInfoMapper.insertConversationName(info);
    }


    @Transactional
    public void updateConversation(Conversation conv) {
        conv.setUpdatedAt(new Date());
        mapper.updateConversation(conv);
    }

    @Transactional
    public void deleteConversation(Long id) {
        mapper.deleteConversation(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        mapper.deleteBatch(ids);
    }

    @Transactional
    public void appendMessageByUuid(String conversationUuid, String messageJson, String messageTimestamp) {
        Map<String, Object> params = new HashMap<>();
        params.put("conversationUuid", conversationUuid);
        params.put("messageJson", messageJson);
        params.put("messageTimestamp", messageTimestamp);
        mapper.appendMessageByUuid(params);
    }
}
