package com.example.onlyone.Controller.Admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.onlyone.DTO.LoginDTO;
import com.example.onlyone.VO.UserVO;
import com.example.onlyone.Common.Result;
import com.example.onlyone.Entity.PageResult;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.LoginService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("admin/user")
@Slf4j
public class UserController {

    @Resource
    private LoginService userService;
    @Resource
    private UserMapper userMapper;

    //插入用户
    @PostMapping("/add")
    public Result addUser(@RequestBody LoginDTO userdto) {
        userService.insertUser(userdto);
        return Result.success();
    }

    @GetMapping
    public Result<PageResult> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        // 使用 MyBatis-Plus 的分页
        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> userPage = userMapper.selectPage(page, null);

        long total = userPage.getTotal();
        List<User> users = userPage.getRecords();

        List<UserVO> userVOS = new ArrayList<>();
        for (User user : users) {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            if (user.getCreateTime() != null) {
                LocalDateTime createTime = user.getCreateTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String createTimeStr = formatter.format(createTime);
                userVO.setCreateTime(createTimeStr);
            }
            userVOS.add(userVO);
        }
        return Result.success(new PageResult(total,userVOS));
    }


    @GetMapping("/{id}")
    public Result getUserById(@PathVariable("id") Long id) {

        return Result.success();
    }

    

}
