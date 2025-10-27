package com.example.service;

import com.example.entity.ConversationInfo;
import com.example.mapper.postgres.ConversationInfoMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationInfoService {

    @Resource
    private ConversationInfoMapper mapper;

    public List<ConversationInfo> selectByUserId(Long userId) {
        return mapper.selectByUserId(userId);
    }

    public ConversationInfo selectById(Long id) {
        return mapper.selectById(id);
    }

    @Transactional
    public void addConversationName(ConversationInfo cn) {
        mapper.insertConversationName(cn);
    }

    @Transactional
    public void updateConversationName(ConversationInfo cn) {
        mapper.updateConversationName(cn);
    }

    @Transactional
    public void deleteConversationName(Long id) {
        mapper.deleteConversationName(id);
    }
}
