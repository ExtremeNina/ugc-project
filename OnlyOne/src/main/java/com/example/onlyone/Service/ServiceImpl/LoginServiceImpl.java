package com.example.onlyone.Service.ServiceImpl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.onlyone.DTO.QQLoginDTO;
import com.example.onlyone.DTO.RegisterDTO;
import com.example.onlyone.DTO.LoginDTO;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Entity.UserAndRole;
import com.example.onlyone.Mapper.UserAndRoleMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.LoginService;
import com.example.onlyone.Utils.JwtProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class LoginServiceImpl extends ServiceImpl<UserMapper, User> implements LoginService {

    @Resource
    private JwtProvider jwtProvider;
    @Resource
    private AuthenticationManager authenticationManager;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserAndRoleMapper userAndRoleMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private MailSender mailSender;
    //记录登录时间的线程池
    private ExecutorService loginRecordThreadPool;


    //初始化线程池
    @PostConstruct
    private void initThreadPool() {
        loginRecordThreadPool = new ThreadPoolExecutor(
                3,
                5,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(50),
                new ThreadFactory() {
                    private final AtomicInteger atomicInteger = new AtomicInteger(1);
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("login-record-" + atomicInteger.getAndIncrement());
                        return thread;
                    }
                },
                //拒绝策略，由主线程执行
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }




    @Override
    public void insertUser(LoginDTO userdto) {

        User user = new User();
        user.setPassword(userdto.getPassword());
        user.setUsername(userdto.getUsername());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        this.save(user);
    }

    @Override
    public String login(LoginDTO userDTO) {

        if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
            log.error("用户名或者密码未输入");
            return null;
        }
        //封装未认证对象
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDTO.getUsername(), userDTO.getPassword());
        //验证认证对象
        Authentication authenticate = authenticationManager.authenticate(authentication);
        //保存到用户上下文中
        //目的是为了让后续请求判断登录状态
        //SecurityContextHolder.getContext().setAuthentication(authenticate);
        //生成jwt
        String JwtToken = jwtProvider.generateToken(authenticate);
        log.info("登录的token:"+JwtToken);

        //异步执行任务
        recordLoginTime(userDTO.getUsername());

        return JwtToken;
    }

    @Override
    public String setCode(RegisterDTO registerDTO) {
        try {
            String code = RandomUtil.randomNumbers(6);
            String key = "login:code" + registerDTO.getUsername();
            stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
            log.info("验证码已发送给用户: {}, 验证码: {}", registerDTO.getUsername(), code);
            return code;
        }catch (Exception e){
            log.error("发送验证码失败 - 用户: {}, 错误: {}", registerDTO.getUsername(), e.getMessage());
            return null;
        }
    }

    @Override
    public void registerUser(RegisterDTO registerDTO) {
        if(registerDTO.getUsername().isEmpty() || registerDTO.getOnePassword().isEmpty() || registerDTO.getTwoPassword().isEmpty()) {
            log.error("请把信息填写完整");
            throw new IllegalArgumentException("请把信息填写完整");
        }
        if (!registerDTO.getOnePassword().equals(registerDTO.getTwoPassword())) {
            throw new IllegalArgumentException("两次密码输入不一致");
        }
        String key = "login:code" + registerDTO.getUsername();
        String code = stringRedisTemplate.opsForValue().get(key);
        if(registerDTO.getCode() == null || !registerDTO.getCode().equals(code)) {
            throw new IllegalArgumentException("验证码错误");
        }
        //插入用户表
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(registerDTO.getOnePassword());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(1);
        this.save(user);
        //插入进用户角色表
        UserAndRole userAndRole = new UserAndRole();
        userAndRole.setUserId(user.getId());
        userAndRole.setRoleId(1L);
        userAndRole.setCreateTime(LocalDateTime.now());
        userAndRoleMapper.insert(userAndRole);
    }

    @Override
    public void loginOut(HttpServletRequest request) {
        //获取jwt令牌
        String jwtToken = getJwtFromRequest(request);
        String username = jwtProvider.getUsernameFromToken(jwtToken);
        //设置黑名单

        // 基于Token本身设置黑名单（使用MD5哈希作为键）
        String tokenHash = DigestUtils.md5DigestAsHex(jwtToken.getBytes());
        String jwtKey = "black:jwt" +  tokenHash;
        Date expiration = jwtProvider.getExpirationDateFromToken(jwtToken);
        Long TTL = expiration.getTime() - System.currentTimeMillis();

        stringRedisTemplate.opsForValue().set(jwtKey,jwtToken,TTL, TimeUnit.MILLISECONDS);
        System.out.println("登出成功");
        SecurityContextHolder.clearContext();

    }


    //发送验证码到用户邮箱上
    @Override
    public Boolean sendQQEmail(String email) {

        boolean flag = false;

        if (!isValidQQEmail(email)) {
            log.info("邮箱格式不正确: {}", email);
            return flag;
        }
        try {
            //生成6位数验证码
            String code = RandomUtil.randomNumbers(6);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("3550478009@qq.com");
            message.setTo(email);
            message.setSubject("您的验证码是");
            message.setText("您的验证码是：" + code);
            //发送验证码
            mailSender.send(message);

            //存入redis中
            String key = "QQlogin:code" + email;
            stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

            flag = true;
            log.info("发送邮箱验证码成功");
            return flag;
        }catch (Exception e){
            log.info("发送邮箱验证码失败");
        }
        return false;
    }


    //QQ登录接口
    @Override
    @Transactional
    public String qqLogin(QQLoginDTO qqLoginDTO) {
        //获取邮箱
        String email = qqLoginDTO.getMail();
        String key = "QQlogin:code" + email;
        if (!stringRedisTemplate.hasKey(key)) {
            log.error("邮箱错误");
            return null;
        }

        String cacheCode = stringRedisTemplate.opsForValue().get(key);
        if (!Objects.equals(cacheCode, qqLoginDTO.getCode())) {
            log.error("验证码错误");
            return null;
        }

        User old = userMapper.selectByMail(qqLoginDTO.getMail());
        if (old != null) {
            log.info("用户之前登录过");

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(old.getUsername(), old.getPassword());
            //验证认证对象
            Authentication authenticate = authenticationManager.authenticate(authentication);
            //生成jwtToken
            String JwtToken = jwtProvider.generateToken(authenticate);
            log.info("登录成功");
            //异步记录登录时间
            recordLoginTime(old.getUsername());

            return JwtToken;

        }

        //验证成功，创建使用用户默认基本信息
        String number = RandomUtil.randomNumbers(6);

        User user = new User();
        user.setUsername("用户"+number);
        user.setPassword("123456");
        user.setMailbox(qqLoginDTO.getMail());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(1);
        this.save(user);
        //插入进用户角色表
        UserAndRole userAndRole = new UserAndRole();
        userAndRole.setUserId(user.getId());
        userAndRole.setRoleId(1L);
        userAndRole.setCreateTime(LocalDateTime.now());
        userAndRoleMapper.insert(userAndRole);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        //验证认证对象
        Authentication authenticate = authenticationManager.authenticate(authentication);
        //生成jwtToken
        String JwtToken = jwtProvider.generateToken(authenticate);
        log.info("创建用户且登录成功");

        //异步更新登录时间
        recordLoginTime(user.getUsername());

        return JwtToken;
    }


    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    //异步执行记录登录时间
    private void recordLoginTime(String username) {

        loginRecordThreadPool.submit(() -> {
            LocalDateTime now = LocalDateTime.now();
            try{
                updateLastLoginTime(username,now);
                log.info("当前用户的上一次登录时间为：{}",now);

            }catch (Exception e){
                log.error("记录用户登录时间异常, username: {}", username, e);
            }


        });
    }

    /**
     * 更新用户最后登录时间
     */
    public void updateLastLoginTime(String username, LocalDateTime loginTime) {
        try {
            // 方法1: 使用自定义查询
            userMapper.updateLastLoginTime(username,loginTime);
            log.info("更新用户登录时间成功{}",loginTime);

        } catch (Exception e) {
            log.error("更新用户登录时间失败, username: {}", username, e);
            throw e; // 抛出异常让调用方处理
        }
    }

    //校验qq邮箱格式
    private boolean isValidQQEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // QQ邮箱正则表达式
        // 匹配格式：数字QQ号@qq.com 或 英文邮箱@qq.com
        String qqEmailRegex = "^(?:[1-9][0-9]{4,10}@qq\\.com|[a-zA-Z0-9._%+-]+@qq\\.com)$";
        return email.trim().toLowerCase().matches(qqEmailRegex);
    }

}