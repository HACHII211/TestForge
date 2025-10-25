package com.example.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class SimpleToolFunctions {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String getWeather(String city) {
        // 这里返回模拟数据，实际可接第三方API
        return city + "今天天气晴气温25℃。";
    }

    public static String getStockPrice(String ticker) {
        // 这里返回模拟数据，实际可接第三方API
        return ticker + "当前股价为100.5元。";
    }

    /**
     * 生成或更新单个测试用例。
     *
     * @param testCaseJson 单个测试用例的 JSON 字符串，
     *                     必须包含 case_id、title、preconditions、steps、expected_result 等字段。
     * @return 填充了 actual_result（空字符串）的用例 JSON 字符串。
     */
    public static String generateTestCase(String testCaseJson) throws IOException {
        JsonNode node = MAPPER.readTree(testCaseJson);
        if (!(node instanceof ObjectNode)) {
            throw new IllegalArgumentException("输入必须是 JSON 对象");
        }
        ObjectNode testCase = (ObjectNode) node;

        // 如果没有 actual_result 字段，则添加；已有则覆盖为空
        testCase.put("actual_result", "");

        // 返回完整的用例 JSON
        return MAPPER.writeValueAsString(testCase);
    }

    /**
     * 批量生成或更新多个测试用例。
     *
     * @param testCasesJson 测试用例数组的 JSON 字符串，
     *                      数组中每个元素格式同单个测试用例。
     * @return 含 actual_result（空字符串）的测试用例 JSON 数组字符串。
     */
    public static String generateTestCases(String testCasesJson) throws IOException {
        JsonNode root = MAPPER.readTree(testCasesJson);
        if (!(root instanceof ArrayNode)) {
            throw new IllegalArgumentException("输入必须是 JSON 数组");
        }
        ArrayNode array = (ArrayNode) root;
        ArrayNode resultArray = MAPPER.createArrayNode();

        for (JsonNode item : array) {
            if (!(item instanceof ObjectNode)) {
                continue; // 或者抛出异常，根据需求
            }
            ObjectNode tc = (ObjectNode) item;
            tc.put("actual_result", "");
            resultArray.add(tc);
        }

        // 返回更新后的用例列表
        return MAPPER.writeValueAsString(resultArray);
    }
}
