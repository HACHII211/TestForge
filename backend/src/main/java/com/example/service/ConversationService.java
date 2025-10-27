package com.example.service;

import com.example.entity.Conversation;
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
        // ensure conversationUuid exists
        if (conv.getConversationUuid() == null || conv.getConversationUuid().isEmpty()) {
            conv.setConversationUuid(UUID.randomUUID().toString());
        }
        if (conv.getCreatedAt() == null) conv.setCreatedAt(new Date());
        if (conv.getUpdatedAt() == null) conv.setUpdatedAt(new Date());
        mapper.insertConversation(conv);
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

    /**
     * Append a message (messageJson is JSON text representing one message object).
     * messageTimestamp may be null; in that case DB will use now() to update last_activity.
     */
    @Transactional
    public void appendMessageByUuid(String conversationUuid, String messageJson, String messageTimestamp) {
        Map<String, Object> params = new HashMap<>();
        params.put("conversationUuid", conversationUuid);
        params.put("messageJson", messageJson);
        params.put("messageTimestamp", messageTimestamp);
        mapper.appendMessageByUuid(params);
    }
}
