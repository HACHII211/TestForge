package com.example.mapper.postgres;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface TestPostgresMapper {
    // 返回当前数据库时间的文本形式，用来测试连接
    @MapKey("id")
    Map<Integer, Map<String, Object>> selectAll();
}
