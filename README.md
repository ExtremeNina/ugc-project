<div align="center">
  <h1>📕 SweetTato</h1>
  <p>仿小红书社区平台 | 前后端分离架构</p>

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.3.4-4FC08D?logo=vue.js)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.4.1-409EFF?logo=element)](https://element-plus.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.0-DC382D?logo=redis)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-FF6600?logo=rabbitmq)](https://www.rabbitmq.com/)

  <p>
    <a href="#项目概述">项目概述</a> •
    <a href="#系统架构">系统架构</a> •
    <a href="#技术架构">技术架构</a> •
    <a href="#项目结构">项目结构</a> •
    <a href="#功能特性">功能特性</a> •
    <a href="#快速开始">快速开始</a> •
    <a href="#部署指南">部署指南</a>
  </p>
</div>

---

## 📋 项目概述

**SweetTato** 是一个功能完整的仿小红书风格社区平台，基于 Spring Boot3 + vue3 构建前后端分离架构的仿小红书轻社交UGC社区，提供从内容创作到用户交互的完整社区解决方案。
---

## 🏛️ 系统架构

```
      ┌─────────────────────────────────────────────────────────────────────────────────┐
      │                              客户端层 (Client Layer)                             │
      ├───────────────────────────────────┬─────────────────────────────────────────────┤
      │         Web 用户端 (hongshu-web)    │           Web 管理端 (frontend)              │
      │         (Vue 3 + Element Plus)    │        (Vue 3 + Tailwind CSS)              │
      │           localhost:5173          │                                             │
      └───────────────────────────────────┴─────────────────────────────────────────────┘
                                            │
                                            ▼
      ┌─────────────────────────────────────────────────────────────────────────────────┐
      │                              接口层 (API Layer)  :8081                           │
      │                       Spring Boot REST API (18个控制器)                           │
      ├─────────────┬─────────────┬─────────────┬─────────────┬─────────────┬───────────┤
      │  用户系统    │  内容系统    │  社交系统    │  消息系统    │  搜索推荐    │  系统管理  │
      │  /api/auth  │  /api/      │  /api/      │  /api/      │  /api/      │  /sys/    │
      │  /api/oauth │   articles  │  Interaction│  private    │  search     │  /admin/  │
      │  /api/users │  /api/      │  /api/      │  /api/      │  /api/      │  /api/    │
      │             │   comment   │  community  │  message    │  recommend  │  admin/   │
      │             │  /api/      │  /api/      │  websocket  │  /api/      │           │
      │             │  create-    │  dynamic    │             │  dynamic    │           │
      │             │  center     │             │             │             │           │
      └─────────────┴─────────────┴─────────────┴─────────────┴─────────────┴───────────┘
                                            │
        ┌───────────────────────────────────┼───────────────────────────────────┐
        │                                   │                                   │
        ▼                                   ▼                                   ▼
   ┌─────────────────────┐           ┌─────────────────────┐           ┌─────────────────────┐
   │      数据层          │           │      缓存层          │           │     消息队列         │
   │     MySQL 8.0       │           │     Redis 7.0       │           │    RabbitMQ 3.12    │
   │ 20+ 数据表           │           │  会话/令牌/Lua 原子   │           │  Feed推送/审核/点赞  │
   │                     │           │  推荐候选集/ZSet 时   │           │  关注/评论/离线消    │
   │                     │           │  间流/协同过滤相似度    │           │  息/文章推送         │
   └─────────────────────┘           └─────────────────────┘           └─────────────────────┘
            ┌───────────────────────────────────┬───────────────────────────────────┐
            │                                   │                                   │
            ▼                                   ▼                                   ▼
  ┌─────────────────────┐           ┌─────────────────────┐           ┌─────────────────────┐
  │      对象存储        │           │      AI 能力         │           │     第三方服务        │
  │      MinIO (S3)     │           │  阿里云内容安全       │           │  QQ 邮件 / GitHub    │
  │   图片/文件存储       │           │  敏感词 AC 自动机     │           │    OAuth 登录        │
  └─────────────────────┘           └─────────────────────┘           └─────────────────────┘
```

### ✨ 项目亮点

- 🔐 **双 Token 认证**: Access Token + Refresh Token 双令牌机制，自动续期防过期，Redis 存储 RT 防重放攻击
- 🛡️ **AI 智能审核**: 敏感词 AC 自动机预检 + 阿里云内容安全 API + 人工复核，三重安全保障
- 📡 **Feed 流设计**: 发布即推送 (Push) 模式 + Redis ZSet 时间线 + 7 天过期自动淘汰
- 💬 **实时私信**: WebSocket 全双工通信 + 在线状态追踪 + 离线消息持久化 + 未读计数
- 🎯 **轻量推荐**: ItemCF 协同过滤 + 多路召回 + 按需补货，30 分钟候选集自动轮换
- ⚡ **高性能**: Redis 缓存 + Lua 原子脚本 + RabbitMQ 异步处理 + Redisson 分布式锁

---

## 🛠️ 技术架构

<table>
<tr>
<td valign="top" width="50%">

### 🔧 后端技术栈

| 技术 | 版本 | 说明 |
|---|---|---|
| **Spring Boot** | 3.5.7 | 基础框架 |
| **Java** | 21 | 开发语言 |
| **Spring Security** | 6.x | 安全框架 + JWT |
| **MyBatis-Plus** | 3.5.7 | ORM 框架 |
| **MySQL** | 8.0 | 主数据库 |
| **Redis** | 7.0 | 缓存 + 分布式锁 |
| **Redisson** | 3.25.2 | 分布式锁框架 |
| **RabbitMQ** | 3.12 | 消息队列 |
| **MinIO** | 8.5.5 | 对象存储 |
| **Spring WebSocket** | 6.x | 实时通信 |
| **Spring Mail** | 3.x | QQ 邮件服务 |
| **Spring OAuth2 Client** | 6.x | GitHub 第三方登录 |
| **阿里云内容安全** | 2.0.6 | AI 内容审核 |

| 工具 | 版本 |
|---|---|
| Lombok | 1.18.x |
| Hutool | 5.8.22 |
| Jsoup | 1.17.1 |
| PageHelper | 1.2.12 |
| jjwt | 0.11.2 |
| OkHttp | 4.12.0 |

</td>
<td valign="top" width="50%">

### 🎨 前端技术栈

#### 用户端 (`hongshu-web`)

| 技术 | 版本 | 说明 |
|---|---|---|
| **Vue** | 3.3.4 | 核心框架 |
| **TypeScript** | 5.0 | 类型安全 |
| **Vite** | 4.4.5 | 构建工具 |
| **Element Plus** | 2.4.1 | UI 组件库 |
| **Pinia** | 2.1.7 | 状态管理 |
| **Vue Router** | 4.2.5 | 路由管理 |
| **Axios** | 1.5.1 | HTTP 客户端 |
| **Tailwind CSS** | 3.4.14 | 原子化 CSS |
| **Less + Sass** | — | CSS 预处理器 |
| **GoEasy** | 2.10.14 | 即时通讯 |
| **ZEGO ZIM** | 2.12.0 | 实时消息 |

#### 管理端 (`frontend`)

| 技术 | 版本 | 说明 |
|---|---|---|
| **Vue** | 3.4.0 | 核心框架 |
| **Vite** | 5.4.0 | 构建工具 |
| **Tailwind CSS** | 3.4.0 | 原子化 CSS |
| **Pinia** | 2.1.0 | 状态管理 |
| **Axios** | 1.6.0 | HTTP 客户端 |

</td>
</tr>
</table>

---

## 📁 项目结构

```
BlueBook/
├── README.md                                       # 项目说明文档
│
├── backend/                                        # 后端服务 (Spring Boot + Java 21)
│   ├── pom.xml                                     #   Maven 构建配置
│   ├── 接口文档_详细.md                               #   API 接口文档
│   └── src/main/java/com/example/onlyone/
│       ├── OnlyOneApplication.java                 #   Spring Boot 启动类
│       ├── Common/                                 #   通用响应体 Result
│       ├── Configuration/                          #   配置类 (11个)
│       │   ├── RedisConfig.java                    #     Redis 配置
│       │   ├── SecurityConfig.java                 #     Spring Security 配置
│       │   ├── JwtConfig.java                      #     JWT 配置
│       │   ├── MinioConfig.java                    #     MinIO 配置
│       │   ├── WebSocketConfig.java                #     WebSocket 配置
│       │   ├── RedissonConfig.java                 #     Redisson 分布式锁
│       │   ├── QQMailConfig.java                   #     QQ 邮件配置
│       │   ├── AsyncConfig.java                    #     异步线程池配置
│       │   ├── SSLConfig.java                      #     SSL 配置
│       │   └── MybatisConfig.java                  #     MyBatis 配置
│       ├── Controller/                             #   REST API 控制器 (18个)
│       │   ├── User/                               #     用户端控制器 (13个)
│       │   ├── Admin/                              #     管理端控制器 (3个)
│       │   ├── SysReviewController.java            #     系统审核 API
│       │   └── SysSensitiveWordController.java     #     敏感词管理 API
│       ├── DTO/                                    #   数据传输对象 (11个)
│       ├── Entity/                                 #   实体类 (22个)
│       ├── Exception/                              #   全局异常处理 + 过滤器
│       ├── Mapper/                                 #   MyBatis Mapper 接口 (18个)
│       ├── Service/                                #   服务层
│       │   ├── ServiceImpl/                        #     服务实现 (20个)
│       │   └── *.java                              #     服务接口
│       ├── VO/                                     #   视图对象 (17个)
│       ├── Rabbitmq/                               #   RabbitMQ 配置 + 7 个队列
│       ├── Task/                                   #   定时任务 (4个)
│       │   ├── CleanupTask.java                    #     离线消息清理 (每周日 3:00)
│       │   ├── HotNotesUpdateTask.java             #     热点榜单更新
│       │   ├── ItemCFTrainTask.java                #     协同过滤模型训练
│       │   └── CommonTask.java                     #     通用定时任务
│       ├── Utils/                                  #   工具类 (14个)
│       │   ├── JwtProvider.java                    #     双 Token 生成/校验
│       │   ├── JwtAuthenticationFilter.java        #     JWT 认证过滤器
│       │   ├── MinioUtils.java                     #     MinIO 文件操作
│       │   ├── SecurityUtils.java                  #     安全工具
│       │   ├── RedisKeyUtils.java                  #     Redis Key 管理
│       │   ├── CacheUtils.java                     #     缓存工具
│       │   ├── FollowLuaScript.java                #     关注 Lua 脚本
│       │   └── LoveLuaScript.java                  #     点赞 Lua 脚本
│       ├── websocket/                              #   WebSocket 实时通信
│       │   ├── ForumWebSocketHandler.java          #     私信/审核推送处理器
│       │   └── AuthHandshakeInterceptor.java       #     握手鉴权拦截器
│       └── Properties/                             #   配置属性类
│
├── hongshu-web/                                    # 用户端前端 (Vue 3 + Element Plus)
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   ├── .env.development / .env.production
│   └── src/
│       ├── main.ts                                 #   入口
│       ├── App.vue                                 #   根组件
│       ├── router/index.ts                         #   路由配置
│       ├── store/                                  #   Pinia 状态管理 (4个)
│       ├── api/                                    #   API 请求 (12个模块)
│       ├── components/                             #   公共组件 (7个)
│       ├── views/                                  #   页面组件 (9个)
│       ├── type/                                   #   TypeScript 类型定义
│       ├── utils/                                  #   工具函数
│       ├── constant/                               #   常量
│       ├── mock/                                   #   Mock 数据
│       └── assets/                                 #   静态资源
│
└── frontend/                                       # 管理端前端 (Vue 3 + Tailwind CSS)
    └── src/
        ├── views/                                  #   页面 (9个)
        ├── components/                             #   组件 (导航/布局/卡片)
        ├── api/                                    #   API 接口
        └── utils/                                  #   工具函数
```

---

## ⭐ 功能特性

### 🔐 用户系统

| 功能 | 说明 |
|---|---|
| 多种登录 | 账号密码 + QQ 邮箱验证码 + GitHub OAuth2 |
| 双令牌认证 | JWT Access Token + Refresh Token 自动续期 + 防重放攻击 |
| 安全防护 | Spring Security + JWT 认证过滤器 + 密码加密 |
| 邮件验证 | QQ 邮箱注册验证码 + 登录验证码 |
| 用户主页 | 个人主页 + 笔记列表 + 粉丝/关注数 |
| 创作中心 | 笔记管理 + 草稿箱 + 分类/标签 |

### 💬 社交互动

| 功能 | 说明 |
|---|---|
| 评论系统 | 发布评论 + 嵌套回复 + 敏感词检测 |
| 关注/取关 | Redis Lua 原子操作 + 粉丝列表 |
| 点赞/收藏 | Redis Lua 原子操作 + 点赞列表 |
| 实时私信 | WebSocket 全双工通信 + 在线状态 + 已读/未读 |
| 通知中心 | 点赞/评论/关注/收藏 + 分类消息列表 |
| 实时推送 | WebSocket 消息通知 + 审核结果实时弹窗 |

### 📝 Feed 流

| 功能 | 说明 |
|---|---|
| 关注动态 | Redis ZSet 时间线，展示关注作者的新文章 |
| 瀑布流首页 | 仿小红书瀑布流 + 分类筛选 + 滚动加载 |
| 文章推送 | RabbitMQ 异步推送新文章到粉丝时间线 |
| 7 天淘汰 | 时间线自动清理 7 天前的文章 |
| 懒人模式 | 新用户从数据库回源加载，活跃用户走 Redis 热路径 |

### 🎯 推荐系统

| 功能 | 说明 |
|---|---|
| 协同过滤 | ItemCF 物品协同过滤模型 + 定时离线训练 |
| 多路召回 | 协同过滤 + 热门 + 标签，三路召回合并得分 |
| 候选集缓存 | 30 分钟 TTL 自动轮换，按需异步补货 |
| 已展示去重 | 已推荐列表持久化，避免重复推荐 |
| 兜底降级 | 候选不足时降级为纯热门推荐 |

### 🛡️ AI 内容审核

| 功能 | 说明 |
|---|---|
| 敏感词过滤 | AC 自动机实时匹配 + 热重载词库 |
| AI 自动审核 | 阿里云内容安全 API + 3 次重试 + 自动降级人工 |
| 人工复核 | 待审列表 + 通过/驳回 + WebSocket 推送结果 |
| 审核弹窗 | 用户端收到审核结果后 ElMessage 实时通知 |
| 离线通知 | 用户不在线时存入离线消息表，上线后推送 |

### 🔧 管理后台

| 功能 | 说明 |
|---|---|
| 用户管理 | 用户列表 + 禁用/启用 |
| 文章管理 | 文章列表 + 状态管理 |
| 审核管理 | 待审列表 + 审批/驳回 + 备注 |
| 敏感词管理 | 增删查 + AC 自动机热重载 |
| 仪表盘 | 数据统计概览 |

---

## 🚀 快速开始

### 前置依赖

| 依赖 | 版本要求 | 说明 |
|---|---|---|
| JDK | 21 | Java 开发环境 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.0+ | 缓存 |
| RabbitMQ | 3.12+ | 消息队列 |
| MinIO | 最新版 | 对象存储 |

### 1. 克隆项目

```bash
git clone <仓库地址>
cd BlueBook
```

### 2. 配置后端

编辑 `backend/src/main/resources/application.yaml`，填写以下关键配置：

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/only?useSSL=false&serverTimezone=UTC
    username: root
    password: 你的密码
  data:
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    username: 你的用户名
    password: 你的密码
    port: 5672

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: update-images

jwt:
  secret: 至少32位的密钥字符串
  access-expiration: 1800000
  refresh-expiration: 604800000

aliyun:
  moderation:
    access-key-id: 你的阿里云AK
    access-key-secret: 你的阿里云SK
    region-id: cn-shanghai
```

### 3. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS `only` DEFAULT CHARACTER SET utf8mb4;
```

启动后端后 MyBatis-Plus 会根据 Entity 自动创建表结构。

### 4. 启动后端

```bash
cd backend

# 编译
mvn clean package -DskipTests

# 启动
mvn spring-boot:run
```

后端启动后访问 `http://localhost:8081`，看到 Spring Boot 启动日志即表示成功。

### 5. 启动用户端前端

```bash
cd hongshu-web

# 安装依赖
npm install

# 开发模式启动
npm run dev
```

访问 `http://localhost:5173` 即可看到仿小红书首页。

### 6. 启动管理端前端

```bash
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev
```

---

## 🚢 部署指南

### 后端部署

```bash
cd backend
mvn clean package -DskipTests
java -jar target/OnlyOne-0.0.1-SNAPSHOT.jar
```

### 前端部署

```bash
# 用户端
cd hongshu-web
npm run build
# 将 dist/ 部署到 Nginx

# 管理端
cd frontend
npm run build
# 将 dist/ 部署到 Nginx
```

### 环境变量

| 变量 | 说明 |
|---|---|
| `VITE_APP_BASE_API` | 用户端 API 地址 (`.env.production`) |
| `ALIYUN_ACCESS_KEY_ID` | 阿里云 AccessKey ID |
| `ALIYUN_ACCESS_KEY_SECRET` | 阿里云 AccessKey Secret |

---

## 📝 命名规范

| 约定 | 说明 |
|---|---|
| `Sys` 前缀 | 系统管理端实现类 (`SysReviewController`, `SysReviewService`, `SysReviewVO`, `SysSensitiveWordController`) |
| `Admin/` 包 | 管理端控制器子包 (`Controller/Admin/`) |
| `User/` 包 | 用户端控制器子包 (`Controller/User/`) |
| `DTO` | 数据传输对象 (`DTO/` 包) |
| `VO` | 视图对象 (`VO/` 包) |
| `Entity` | 数据库实体 (`Entity/` 包) |

---

## 🧪 测试

```bash
cd backend

# 运行全部测试
mvn test

# 运行指定测试类
mvn test -Dtest=LoginServiceImplTest
mvn test -Dtest=CreateCenterServiceImplTest
mvn test -Dtest=CommentServiceImplTest
```

---

## 📄 许可证

请在仓库中添加合适的 `LICENSE` 文件。

## 📧 联系方式

如需帮助，请在仓库中创建 Issue 或联系维护者。
