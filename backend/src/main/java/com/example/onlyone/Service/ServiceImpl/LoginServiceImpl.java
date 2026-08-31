package com.example.onlyone.Service.ServiceImpl;

import cn.hutool.core.util.RandomUtil;
import com.example.onlyone.DTO.QQLoginDTO;
import com.example.onlyone.DTO.RegisterDTO;
import com.example.onlyone.DTO.LoginDTO;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Entity.UserAndRole;
import com.example.onlyone.Mapper.UserAndRoleMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Properties.JwtProperties;
import com.example.onlyone.Service.LoginService;
import com.example.onlyone.Utils.JwtProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

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
    @Resource
    private JwtProperties jwtProperties;
    @Resource
    private PasswordEncoder passwordEncoder;

    private ExecutorService loginRecordThreadPool;

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
        userMapper.insert(user);
    }

    @Override
    public Map<String, String> login(LoginDTO userDTO, HttpServletResponse response) {
        if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
            log.error("用户名或者密码未输入");
            return null;
        }
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDTO.getUsername(), userDTO.getPassword());
        Authentication authenticate = authenticationManager.authenticate(authentication);

        String deviceId = userDTO.getDeviceId();
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = "unknown";
        }

        String accessToken = jwtProvider.generateAccessToken(authenticate, deviceId);
        String refreshToken = jwtProvider.generateRefreshToken(authenticate, deviceId);

        saveRefreshToken(authenticate, refreshToken);
        setRefreshTokenCookie(response, refreshToken);

        log.info("登录成功，AT jti: {}, RT jti: {}, deviceId: {}", jwtProvider.getJtiFromToken(accessToken), jwtProvider.getJtiFromToken(refreshToken), deviceId);
        recordLoginTime(userDTO.getUsername());

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("expiresIn", String.valueOf(jwtProperties.getAccessExpiration() / 1000));
        return tokens;
    }

    @Override
    public String login(LoginDTO userDTO) {
        Map<String, String> tokens = login(userDTO, null);
        return tokens != null ? tokens.get("accessToken") : null;
    }

    @Override
    public Map<String, String> qqLogin(QQLoginDTO qqLoginDTO, HttpServletResponse response) {
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
            Authentication authenticate = authenticationManager.authenticate(authentication);

            String accessToken = jwtProvider.generateAccessToken(authenticate);
            String refreshToken = jwtProvider.generateRefreshToken(authenticate);
            saveRefreshToken(authenticate, refreshToken);
            setRefreshTokenCookie(response, refreshToken);

            recordLoginTime(old.getUsername());
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);
            tokens.put("expiresIn", String.valueOf(jwtProperties.getAccessExpiration() / 1000));
            return tokens;
        }

        String number = RandomUtil.randomNumbers(6);
        User user = new User();
        user.setUsername("用户" + number);
        user.setPassword("123456");
        user.setMailbox(qqLoginDTO.getMail());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(1);
        userMapper.insert(user);

        UserAndRole userAndRole = new UserAndRole();
        userAndRole.setUserId(user.getId());
        userAndRole.setRoleId(1L);
        userAndRole.setCreateTime(LocalDateTime.now());
        userAndRoleMapper.insert(userAndRole);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        Authentication authenticate = authenticationManager.authenticate(authentication);

        String accessToken = jwtProvider.generateAccessToken(authenticate);
        String refreshToken = jwtProvider.generateRefreshToken(authenticate);
        saveRefreshToken(authenticate, refreshToken);
        setRefreshTokenCookie(response, refreshToken);

        recordLoginTime(user.getUsername());
        log.info("创建用户且登录成功");

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("expiresIn", String.valueOf(jwtProperties.getAccessExpiration() / 1000));
        return tokens;
    }

    @Override
    @Transactional
    public String qqLogin(QQLoginDTO qqLoginDTO) {
        Map<String, String> tokens = qqLogin(qqLoginDTO, null);
        return tokens != null ? tokens.get("accessToken") : null;
    }

    @Override
    public String setCode(RegisterDTO registerDTO) {
        try {
            String code = RandomUtil.randomNumbers(6);
            String key = "login:code" + registerDTO.getUsername();
            stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
            log.info("验证码已发送给用户: {}, 验证码: {}", registerDTO.getUsername(), code);
            return code;
        } catch (Exception e) {
            log.error("发送验证码失败 - 用户: {}, 错误: {}", registerDTO.getUsername(), e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public void registerUser(RegisterDTO registerDTO) {
        if (registerDTO.getUsername().isEmpty() || registerDTO.getOnePassword().isEmpty() || registerDTO.getTwoPassword().isEmpty()) {
            log.error("请把信息填写完整");
            throw new IllegalArgumentException("请把信息填写完整");
        }
        if (!registerDTO.getOnePassword().equals(registerDTO.getTwoPassword())) {
            throw new IllegalArgumentException("两次密码输入不一致");
        }
        String key = "login:code:" + registerDTO.getUsername();
        String code = stringRedisTemplate.opsForValue().get(key);
        if (registerDTO.getCode() == null || !registerDTO.getCode().equals(code)) {
            throw new IllegalArgumentException("验证码错误");
        }
        stringRedisTemplate.delete(key);
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getOnePassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(1);
        userMapper.insert(user);
        UserAndRole userAndRole = new UserAndRole();
        userAndRole.setUserId(user.getId());
        userAndRole.setRoleId(1L);
        userAndRole.setCreateTime(LocalDateTime.now());
        userAndRoleMapper.insert(userAndRole);
    }

    @Override
    public void loginOut(HttpServletRequest request, HttpServletResponse response) {
        String token = getJwtFromRequest(request);
        if (StringUtils.hasText(token)) {
            try {
                String jti = jwtProvider.getJtiFromToken(token);
                if (jti != null) {
                    Date expiration = jwtProvider.getExpirationDateFromToken(token);
                    long ttl = expiration.getTime() - System.currentTimeMillis();
                    if (ttl > 0) {
                        stringRedisTemplate.opsForValue().set("blacklist:at:" + jti, "1", ttl, TimeUnit.MILLISECONDS);
                    }
                }

                Long userId = jwtProvider.getUserIdFromToken(token);
                String deviceId = jwtProvider.getDeviceIdFromToken(token);
                if (userId != null && deviceId != null) {
                    stringRedisTemplate.delete("rt:user:" + userId + ":" + deviceId);
                    log.info("已清除用户 {} 设备 {} 的 Refresh Token", userId, deviceId);
                }
            } catch (Exception e) {
                log.error("登出时清理 Redis 失败", e);
            }
        }

        clearRefreshTokenCookie(request, response);
        SecurityContextHolder.clearContext();
        log.info("登出完成");
    }

    @Override
    public void loginOut(HttpServletRequest request) {
        loginOut(request, null);
    }

    @Override
    public Boolean sendQQEmail(String email) {
        if (!isValidQQEmail(email)) {
            log.info("邮箱格式不正确: {}", email);
            return false;
        }
        try {
            String code = RandomUtil.randomNumbers(6);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("3550478009@qq.com");
            message.setTo(email);
            message.setSubject("您的验证码是");
            message.setText("您的验证码是：" + code);
            mailSender.send(message);

            String key = "QQlogin:codeL" + email;
            stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
            log.info("发送邮箱验证码成功");
            return true;
        } catch (Exception e) {
            log.info("发送邮箱验证码失败");
        }
        return false;
    }

    public Map<String, String> refreshToken(String refreshToken, HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("Refresh Token 为空");
            return null;
        }

        if (!jwtProvider.validateToken(refreshToken)) {
            log.warn("Refresh Token 无效或已过期");
            return null;
        }

        if (!"refresh".equals(jwtProvider.getTokenType(refreshToken))) {
            log.warn("Token 类型不是 refresh");
            return null;
        }

        Long userId = jwtProvider.getUserIdFromToken(refreshToken);
        String deviceId = jwtProvider.getDeviceIdFromToken(refreshToken);
        String jti = jwtProvider.getJtiFromToken(refreshToken);

        if (userId == null || deviceId == null || jti == null) {
            log.warn("Refresh Token 缺少必要字段");
            return null;
        }

        String redisKey = "rt:user:" + userId + ":" + deviceId;
        String storedJti = stringRedisTemplate.opsForValue().get(redisKey);

        if (!jti.equals(storedJti)) {
            log.warn("Refresh Token 已被轮换或不存在: userId={}, deviceId={}", userId, deviceId);
            if (storedJti != null) {
                stringRedisTemplate.delete(redisKey);
                log.warn("检测到 RT 复用攻击，已清除 device {} 的 Refresh Token", deviceId);
            }
            return null;
        }

        stringRedisTemplate.delete(redisKey);

        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("刷新时用户不存在: userId={}", userId);
            return null;
        }

        String newJti = cn.hutool.core.util.IdUtil.fastSimpleUUID();
        stringRedisTemplate.opsForValue().set(redisKey, newJti, jwtProperties.getRefreshExpiration(), TimeUnit.MILLISECONDS);

        String newAccessToken = generateAccessTokenByUser(user, deviceId, "local");
        String newRefreshToken = generateRefreshTokenByUser(user, deviceId, newJti, "local");

        setRefreshTokenCookie(response, newRefreshToken);

        log.info("Token 刷新成功: userId={}, deviceId={}, oldJti={}, newJti={}", userId, deviceId, jti, newJti);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);
        tokens.put("expiresIn", String.valueOf(jwtProperties.getAccessExpiration() / 1000));
        return tokens;
    }

    private String generateAccessTokenByUser(User user, String deviceId, String source) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties().getAccessExpiration());
        return io.jsonwebtoken.Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("deviceId", deviceId)
                .claim("source", source)
                .claim("type", "access")
                .claim("jti", cn.hutool.core.util.IdUtil.fastSimpleUUID())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtProperties().getSecret().getBytes()), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateRefreshTokenByUser(User user, String deviceId, String jti, String source) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties().getRefreshExpiration());
        return io.jsonwebtoken.Jwts.builder()
                .claim("userId", user.getId())
                .claim("deviceId", deviceId)
                .claim("source", source)
                .claim("type", "refresh")
                .claim("jti", jti)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtProperties().getSecret().getBytes()), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }



    private void saveRefreshToken(Authentication authenticate, String refreshToken) {
        Long userId = null;
        Object principal = authenticate.getPrincipal();
        if (principal instanceof com.example.onlyone.Entity.UserDetail) {
            userId = ((com.example.onlyone.Entity.UserDetail) principal).getUserId();
        }
        if (userId == null) return;

        String jti = jwtProvider.getJtiFromToken(refreshToken);
        String deviceId = jwtProvider.getDeviceIdFromToken(refreshToken);
        if (deviceId == null) return;

        stringRedisTemplate.opsForValue().set(
                "rt:user:" + userId + ":" + deviceId,
                jti,
                jwtProperties.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );
    }

    public void saveRefreshToken(Long userId, String refreshToken) {
        if (userId == null || refreshToken == null) return;
        String jti = jwtProvider.getJtiFromToken(refreshToken);
        String deviceId = jwtProvider.getDeviceIdFromToken(refreshToken);
        if (deviceId == null) deviceId = "oauth";
        stringRedisTemplate.opsForValue().set(
                "rt:user:" + userId + ":" + deviceId,
                jti,
                jwtProperties.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (response == null) return;
        long expirySeconds = jwtProperties().getRefreshExpiration() / 1000;

        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) expirySeconds);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
        log.info("Refresh Token Cookie 已设置");
    }

    public void clearRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        if (response == null) return;
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private JwtProperties jwtProperties() {
        return com.example.onlyone.Utils.SpringContextUtils.getBean(
                com.example.onlyone.Properties.JwtProperties.class);
    }



    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void recordLoginTime(String username) {
        loginRecordThreadPool.submit(() -> {
            LocalDateTime now = LocalDateTime.now();
            try {
                updateLastLoginTime(username, now);
                log.info("当前用户的上一次登录时间为：{}", now);
            } catch (Exception e) {
                log.error("记录用户登录时间异常, username: {}", username, e);
            }
        });
    }

    public void updateLastLoginTime(String username, LocalDateTime loginTime) {
        try {
            userMapper.updateLastLoginTime(username, loginTime);
            log.info("更新用户登录时间成功{}", loginTime);
        } catch (Exception e) {
            log.error("更新用户登录时间失败, username: {}", username, e);
            throw e;
        }
    }

    private boolean isValidQQEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String qqEmailRegex = "^(?:[1-9][0-9]{4,10}@qq\\.com|[a-zA-Z0-9._%+-]+@qq\\.com)$";
        return email.trim().toLowerCase().matches(qqEmailRegex);
    }
}
