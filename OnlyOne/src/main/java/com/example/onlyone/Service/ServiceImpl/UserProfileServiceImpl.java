package com.example.onlyone.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.onlyone.DTO.UpdateUserMessageDTO;
import com.example.onlyone.VO.UpdateUserMessageVO;
import com.example.onlyone.VO.UserProfileVO;
import com.example.onlyone.Entity.Article;
import com.example.onlyone.Entity.User;
import com.example.onlyone.Mapper.ArticleMapper;
import com.example.onlyone.Mapper.FollowMapper;
import com.example.onlyone.Mapper.UserLoveMapper;
import com.example.onlyone.Mapper.UserMapper;
import com.example.onlyone.Service.LoveService;
import com.example.onlyone.Service.UserProfileService;
import com.example.onlyone.Utils.MinioUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserProfileServiceImpl extends ServiceImpl<UserMapper, User> implements UserProfileService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private MinioUtils minioUtils;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private UserLoveMapper userLoveMapper;
    @Resource
    private FollowMapper followMapper;
    @Resource
    private LoveService loveService;

    // 缓存过期时间
    private static final long CACHE_TTL = 7 * 24 * 60 * 60L; // 7天


    //个人界面显示的用户信息
    @Override
    public UserProfileVO getUserMessage() {
        //从上下文中获取用户名
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        //根据用户名查询用户信息
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }

        Long userId = user.getId();

        //封装成vo对象返回
        UserProfileVO userProfileVO = new UserProfileVO();

        // 1. 获取用户的粉丝数（fan）
        userProfileVO.setFan(getFansCountFromCacheOrDB(userId));
        // 2. 获取用户的关注数（follows）
        userProfileVO.setFollows(getFollowingCountFromCacheOrDB(userId));
        // 3。获取当前用户总点赞数
        userProfileVO.setLove(getAllLoveCount(userId));

        userProfileVO.setIsFollow(false);

        BeanUtils.copyProperties(user,userProfileVO);
        log.info("userProfileVO:{}",userProfileVO);
        return userProfileVO;
    }

    //编辑个人资料模块
    @Override
    public void updateUserMessage(UpdateUserMessageDTO updateUserMessageDTO) {

        // 1. 参数校验
        if (updateUserMessageDTO == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        //从上下文中获取用户名
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        //根据用户名查询用户信息
        User user = userMapper.selectByUsername(username);
        log.info("旧的头像:{}",user.getIcon());
        //将新的用户信息复制到user对象中
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        //将新的用户信息拷贝到user对象中
        BeanUtils.copyProperties(updateUserMessageDTO, user);
        //将更新后的user对象保持到数据库中
        userMapper.updateById(user);
        log.info("新的头像:"+user.getIcon());
        log.info("保存成功");
    }

    @Override
    public Map<String, String> uploadImage(MultipartFile file) {
        try {
            //获取minion中的url
            String bucketName = "icon-upload";
            String ImageUrl = uploadCoverImage(file,bucketName);
            Map<String, String> map = new HashMap<>();
            //封装成map对象返回
            map.put("url", ImageUrl);
            map.put("message", "图片上传成功");
            log.info("这是后端返回的url"+map.get("url"));
            return map;
        }catch (Exception e){
            log.error("图片上传失败"+e.getMessage());
            return null;
        }
    }


    //编辑个人资料页面回显
    @Override
    public UpdateUserMessageVO showUserMessage() {
        //获取上下文认证
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        //根据用户名查询用户信息
        User user = userMapper.selectByUsername(username);
        UpdateUserMessageVO updateUserMessageVO = new UpdateUserMessageVO();
        BeanUtils.copyProperties(user,updateUserMessageVO);
        return updateUserMessageVO;
    }


    //上传图片到minio
    private String uploadCoverImage(MultipartFile coverImage, String bucketName) {

        if (coverImage == null || coverImage.isEmpty()) {
            log.info("不上传图片，则默认无封面");
            return null;
        }

        try {
            //验证图片类型
            validDateImage(coverImage);
            //上传到minio中
            Map<String,String> resultMap = minioUtils.uploadFile(coverImage,bucketName);
            if (resultMap == null || resultMap.get("url") == null) {
                throw new RuntimeException("封面图片上传失败");
            }
            //上传成功返回minio中的url
            log.info("图片上传成功");
            return resultMap.get("url");

        }catch (Exception e){
            log.error("封面图片上传失败: {}", e.getMessage());
            throw new RuntimeException("封面图片上传失败: " + e.getMessage());
        }

    }


    //验证图类型
    private void validDateImage(MultipartFile file) {

        String originalFilename = file.getOriginalFilename();

        String extension = null;
        if (originalFilename != null) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("只支持以下图片格式: " + String.join(", ", allowedExtensions));
        }

        // 通过Content-Type进一步验证
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("文件类型不是图片");
        }

    }


    /**
     * 从缓存或数据库获取粉丝数
     */
    public Long getFansCountFromCacheOrDB(Long userId) {
        String fansCountKey = String.format("user:fansCount:%s", userId);

        // 尝试从Redis获取
        String fansCountStr = stringRedisTemplate.opsForValue().get(fansCountKey);

        if (StringUtils.hasText(fansCountStr)) {

            log.info("{}用户的关注数为{}", userId, fansCountStr);
            return Long.parseLong(fansCountStr);
        }

        // Redis中没有，从数据库获取
        Long fansCount = userLoveMapper.selectByUserId(userId);
        if (fansCount == null) {
            fansCount = 0L;
        }

        // 存入Redis，设置过期时间
        stringRedisTemplate.opsForValue().set(fansCountKey, fansCount.toString(), 7, TimeUnit.DAYS);
        log.info("{}用户的关注数为{}", userId, fansCount);
        return fansCount;
    }

    /**
     * 从缓存或数据库获取关注数
     */
    public Long getFollowingCountFromCacheOrDB(Long userId) {
        String followingCountKey = String.format("user:followingCount:%s", userId);

        // 尝试从Redis获取
        String followingCountStr = stringRedisTemplate.opsForValue().get(followingCountKey);

        if (StringUtils.hasText(followingCountStr)) {
            log.info("{}用户的关注数为{}", userId, followingCountStr);
            return Long.parseLong(followingCountStr);
        }

        // Redis中没有，从数据库获取
        Long followingCount = followMapper.countFollowing(userId);
        if (followingCount == null) {
            followingCount = 0L;
        }

        // 存入Redis，设置过期时间
        stringRedisTemplate.opsForValue().set(followingCountKey, followingCount.toString(), 7, TimeUnit.DAYS);
        log.info("{}用户的关注数为{}", userId, followingCount);
        return followingCount;
    }


    /**
     * 从缓存或数据库获取总点赞数
     */
    public Long getAllLoveCount(Long userId) {

        //获取用户全部文章
        List<Article> articleList = articleMapper.selectByAuthorId(userId);

        if (articleList == null || articleList.isEmpty()) {
            return 0L;
        }

        Long totalLoveCount = 0L;

        for (Article article : articleList) {
            //获取每一篇文章的点赞数
            String loveCountStr = loveService.getEntityLoveCount(article.getId(),1L);
            Long loveCount = Long.parseLong(loveCountStr);
            totalLoveCount += loveCount;

        }

        return totalLoveCount;
    }

}
