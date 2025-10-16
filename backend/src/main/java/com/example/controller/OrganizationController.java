package com.example.controller;
import com.example.service.OrganizationService;
import com.example.common.Result;
import com.example.entity.User;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/organization")

public class OrganizationController {
    @Resource
    private OrganizationService organizationService;

    @GetMapping("/users")
    public Result selectUsers(
            User user,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageInfo<User> pageInfo;
        if (user != null) {
            pageInfo = organizationService.selectByFilters(user, pageNum, pageSize);
        } else {
            pageInfo = organizationService.selectByPage(pageNum, pageSize);
        }
        return Result.success(pageInfo);
    }


    @PostMapping("/users")
    public Result createUserWithRole(@RequestBody User user){
        organizationService.createUserWithRole(user);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    public Result deleteUser(@PathVariable Integer id){
        organizationService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/users/{id}")
    public Result updateUserWithRole(@RequestBody User user){
        System.out.println(user);
        organizationService.updateUserWithRole(user);
        return Result.success();
    }
}
