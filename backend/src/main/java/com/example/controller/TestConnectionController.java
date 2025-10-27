package com.example.controller;

import com.example.mapper.postgres.TestPostgresMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestConnectionController {

    private final TestPostgresMapper testPostgresMapper;

    public TestConnectionController(TestPostgresMapper testPostgresMapper) {
        this.testPostgresMapper = testPostgresMapper;
    }

    @GetMapping("/test/postgres-now")
    public
    Map<Integer, Map<String, Object>> selectAll() {
        return testPostgresMapper.selectAll();
    }
}
