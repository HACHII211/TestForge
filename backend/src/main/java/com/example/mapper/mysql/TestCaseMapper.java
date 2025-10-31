package com.example.mapper.mysql;

import com.example.entity.TestCase;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TestCaseMapper {
    List<TestCase> selectAll();

    List<TestCase> selectByTitle(String title);

    TestCase selectById(Integer id);

    void insert(TestCase testCase);

    int insertBatch(@Param("list") List<com.example.entity.TestCase> list);

    void update(TestCase testCase);

    void delete(Integer id);
}
