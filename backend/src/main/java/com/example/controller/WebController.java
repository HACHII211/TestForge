//package com.example.controller;
//import cn.hutool.captcha.ShearCaptcha;
//import cn.hutool.core.date.DateField;
//import cn.hutool.core.date.DateTime;
//import cn.hutool.core.date.DateUtil;
//import com.example.service.AdminService;
//import com.example.service.ArticleService;
//import com.example.service.CaptchaService;
//import com.example.service.EmployeeService;
//import com.example.common.Result;
//import com.example.entity.*;
//import io.swagger.v3.oas.annotations.Operation;
//import jakarta.annotation.Resource;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.*;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//import org.springframework.web.bind.annotation.RestController;
//import java.util.HashMap;
//import java.util.Map;
//
//
//@Slf4j
//@RestController
//public class WebController {
//
//    @Resource
//    EmployeeService employeeService;
//    @Resource
//    AdminService adminService;
//    @Resource
//    CaptchaService captchaService;
//    @Resource
//    ArticleService articleService;
//
//
//    @PostMapping("/login")
//    public Result login(@RequestBody Account account, HttpServletRequest request, HttpServletResponse response) {
//        Account result = null;
//        String token = null;
//
//        String userAgent = request.getHeader("User-Agent");
//        System.out.println("User-Agent: " + userAgent);  // 打印 User-Agent 信息
//
//        // 你可以在这里检查 User-Agent 是否符合特定要求
//        if (userAgent != null && userAgent.contains("SomeSpecificClient")) {
//            System.out.println("请求来自特定客户端！");
//        } else {
//            System.out.println("请求来自其他客户端！");
//        }
//
//        if ("EMP".equals(account.getRole())) {
//            result = employeeService.login(account);
//        } else if ("ADM".equals(account.getRole())) {
//            result = adminService.login(account);
//        }
//        System.out.println("登录成功！生成token！");
//        token = com.example.utils.JwtUtil.createToken(result);
//
//        // 将token添加到响应头中
//        response.setHeader("Authorization", "Bearer " + token);
//
//        return Result.success(result);
//    }
//    @Autowired
//    private StringRedisTemplate redisTemplate;
//
//    @PostMapping("/register")
//    public Result register(@RequestBody Map<String, String> requestData, HttpServletResponse response, HttpServletRequest request) {
//        // 提取用户信息和验证码
//        String username = requestData.get("userName");
//        String password = requestData.get("password");
//        String captchaInput = requestData.get("captcha");  // 用户提交的验证码
//
//        // 获取用户的 sessionId，作为 Redis 键的一部分
//        String sessionId = request.getSession().getId();
//
//        // 从 Redis 获取存储的验证码
//        System.out.println("captcha:" + sessionId);
//        String redisCaptcha = redisTemplate.opsForValue().get("captcha:" + sessionId);
//        System.out.println("Session ID in /register: " + sessionId);
//        System.out.println("redis captcha " + redisCaptcha);
//
//        // 校验验证码是否正确
//        if (redisCaptcha == null) {
//            return Result.error("500", "验证码已过期");
//        }
//
//        if (!captchaService.validate(captchaInput, redisCaptcha)) {
//            return Result.error("500", "验证码错误");
//        }
//        redisTemplate.delete("captcha:" + sessionId);
//        // 验证码正确，继续执行注册逻辑
//        Employee employee = new Employee();
//        employee.setUserName(username);
//        employee.setPassword(password);
//
//        String token = com.example.utils.JwtUtil.createToken(employee);
//        response.setHeader("Authorization", "Bearer " + token);
//
//        // 调用服务层进行注册
//        return Result.success(employeeService.register(employee));
//    }
//
//
//    @Operation(summary = "更新密码", description = "接收ID与新密码，辨别用户身份提交密码更新操作")
//    @PutMapping("/updatePassword")
//    public Result updatePassword(@RequestBody Account account){
//        if (account.getEmpNo()!=null){
//            Employee employee = employeeService.selectById(account.getEmpNo());
//            employee.setPassword(account.getNewPassword());
//            employeeService.update(employee);
//        } else {
//            Admin admin = adminService.selectById(account.getId());
//            admin.setPassword(account.getNewPassword());
//            adminService.update(admin);
//            return Result.success();
//        }
//        return Result.success();
//    }
//
//    @GetMapping("/getBarData")
//    public Result getBarData() {
//        Map<String,Object> map =new HashMap<>();
//        List<Employee> employees = employeeService.selectAll(null);
//        Set<String> departmentNameList = employees.stream().map(Employee::getDeptName).collect(Collectors.toSet());
//        map.put("department",departmentNameList);
//        List<Long> countList  = new ArrayList<>();
//        for (String deptName : departmentNameList) {
//            long count = employees.stream().filter(employee -> employee.getDeptName().equals(deptName)).count();
//            countList.add(count);
//        }
//        map.put("count",countList);
//        return Result.success(map);
//    }
//
//    @GetMapping("/getLineData")
//    public Result getLineData() {
//        Map<String,Object> map =new HashMap<>();
//        Date now = new Date();
//        DateTime start = DateUtil.offsetDay(now,-7);
//        List<DateTime> dateTimeList = DateUtil.rangeToList(start,now, DateField.DAY_OF_YEAR);
//        List<String> dateStr = dateTimeList.stream()
//                .map(dateTime -> DateUtil.format(dateTime, "MM月dd日"))
//                .sorted(Comparator.naturalOrder())
//                .collect(Collectors.toList());
//        map.put("times",dateStr);
//
//        List<Integer> countList = new ArrayList<>();
//
//        for (DateTime day : dateTimeList) {
//            String dayFormat = DateUtil.formatDate(day);
//            Integer count = articleService.selectCountByDay(dayFormat);
//            countList.add(count);
//        }
//
//        map.put("count",countList);
//        return Result.success(map);
//    }
//
//    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
//    public byte[] getCaptcha(HttpServletRequest request) {
//        // 生成验证码
//        ShearCaptcha captcha = captchaService.generateCaptcha();
//        String captchaCode = captcha.getCode();
//
//        // 获取用户的 Session ID 或使用其他唯一标识符
//        String sessionId = request.getSession().getId();
//
//        // 将验证码存储到 Redis，设置过期时间为 5 分钟（可以根据需求调整）
//        redisTemplate.opsForValue().set("captcha:" + sessionId, captchaCode, 5, TimeUnit.MINUTES);
//
//        // 输出日志
//        System.out.println("Generated captcha code: " + captchaCode);
//        return captcha.getImageBytes();
//    }
//
//    @PostMapping(value = "/validate", produces = "application/json;charset=UTF-8")
//    public Result validateCaptcha(@RequestBody String input, HttpServletRequest request) {
//        // 获取用户的 Session ID
//        String sessionId = request.getSession().getId();
//        // 从 Redis 获取验证码
//        String captchaCode = redisTemplate.opsForValue().get("captcha:" + sessionId);
//
//        System.out.println("Stored captcha code from Redis: " + captchaCode);
//        System.out.println("User input: " + input);
//
//        // 如果验证码已过期（Redis 中没有该验证码）
//        if (captchaCode == null) {
//            System.out.println(captchaCode);
//            return Result.error("500", "验证码已过期");
//        }
//
//        // 验证用户输入的验证码
//        if (captchaService.validate(input, captchaCode)) {
//            // 验证通过后，从 Redis 中删除验证码
//            return Result.success();
//        } else {
//            return Result.error("500", "验证码错误");
//        }
//    }
//}
//
//
//
