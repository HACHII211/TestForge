// 文件: com/example/Service/TestCaseService.java
package com.example.service;

import com.example.entity.TestCase;
import com.example.mapper.mysql.TestCaseMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestCaseService {

    @Resource
    private TestCaseMapper testCaseMapper;

    public PageInfo<TestCase> selectByPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TestCase> list = testCaseMapper.selectAll();
        return PageInfo.of(list);
    }

    public PageInfo<TestCase> selectByTitle(String title, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TestCase> list = testCaseMapper.selectByTitle(title);
        return PageInfo.of(list);
    }

    public TestCase selectById(Integer id) {
        return testCaseMapper.selectById(id);
    }

    public void addTestCase(TestCase testCase) {
        testCaseMapper.insert(testCase);
    }

    public void updateTestCase(TestCase testCase) {
        testCaseMapper.update(testCase);
    }

    public void deleteTestCase(Integer id) {
        testCaseMapper.delete(id);
    }
}
