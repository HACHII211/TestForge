package com.example.mapper;

import com.example.entity.TestCase;
import java.util.List;

public interface TestCaseMapper {
    List<TestCase> selectAll();

    List<TestCase> selectByTitle(String title);

    TestCase selectById(Integer id);

    void insert(TestCase testCase);

    void update(TestCase testCase);

    void delete(Integer id);
}
