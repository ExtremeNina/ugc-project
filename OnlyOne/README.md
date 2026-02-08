**OnlyOne 项目**

简体中文说明文档。

**项目简介**
- **OnlyOne** 是一个基于 Spring Boot 的后端项目，包含用户、文章、评论、关注、点赞等功能模块，使用 MyBatis 做持久层映射，并集成了 Redis、RabbitMQ、MinIO（或对象存储）、QQ 邮件等配置。

**主要特性**
- 用户注册/登录（支持 OAuth2/QQ 登录）
- 文章管理（草稿、发布、分类、标签）
- 评论、私信、关注/取消关注、点赞/取消点赞（使用 Lua 脚本实现原子操作）
- 权限与角色管理
- WebSocket 消息推送

**技术栈**
- Java + Spring Boot
- MyBatis
- Redis
- RabbitMQ
- MinIO（或其他对象存储）
- Maven 构建

**项目结构（重要路径）**
- `pom.xml`：Maven 构建文件
- `src/main/java/com/example/onlyone/OnlyOneApplication.java`：Spring Boot 启动类
- `src/main/java/com/example/onlyone/Configuration/`：各类配置（Redis、JWT、MinIO、MyBatis、Security、WebSocket 等）
- `src/main/java/com/example/onlyone/Controller/`：控制器（分 `Admin`、`User` 子包）
- `src/main/java/com/example/onlyone/Mapper/`：MyBatis 映射接口
- `src/main/java/com/example/onlyone/Entity/`：实体类
- `src/main/resources/application.yaml`：应用配置
- `src/main/resources/mapper/`：MyBatis XML 映射文件（如 `ArticleMapper.xml` 等）
- `src/main/resources/*.lua` 与 `src/main/resources/static/Lua/`：Redis 原子操作的 Lua 脚本（关注/点赞等）

**配置说明**
- 请首先检查并编辑 `src/main/resources/application.yaml`，根据你的环境填写数据库、Redis、RabbitMQ、MinIO（或 S3）、邮件服务等连接信息。
- 常见配置位置：`src/main/java/com/example/onlyone/Configuration/` 下的各个配置类（`RedisConfig.java`, `MinioConfig.java`, `JwtConfig.java`, `QQMailConfig.java` 等）。

**本地运行**
1. 安装并配置好项目所需服务：数据库（MySQL 等）、Redis、RabbitMQ、MinIO（或 S3 兼容存储）。
2. 在项目根目录执行：

```bash
mvn clean package -DskipTests
```

3. 运行应用：

```bash
mvn spring-boot:run
# 或
java -jar target/*.jar
```

4. 访问接口：默认由 `application.yaml` 中配置的端口（通常为 8080）提供服务。

**测试**
- 执行单元/集成测试：

```bash
mvn test
```

**数据库/脚本**
- Mapper XML 文件位于 `src/main/resources/mapper/`。
- Redis 的 Lua 脚本位于 `src/main/resources/`（例如 `LoveLuaScript.lua`, `FollowLuaScript.lua` 等），用于原子性操作。

**打包与部署**
- 使用 `mvn clean package` 打包，生成的 jar 在 `target/` 目录。
- 在生产环境中，建议使用容器化（Docker）或者在 CI/CD 中将 `jar` 与环境变量/配置分离管理。

**常见问题**
- 若启动失败，请查看控制台日志并确认以下服务是否可用：数据库、Redis、RabbitMQ、对象存储。
- 若文件上传失败，请检查 `MinioConfig` 与 `application.yaml` 中的 MinIO/S3 配置。

**贡献**
- 欢迎提交 issue 与 PR。请在提交前运行本地测试并遵循现有代码风格。

**联系 / 许可证**
- 项目许可证：请在仓库中添加合适的 `LICENSE` 文件（如果需要）。
- 如需帮助，请在仓库中创建 issue 或联系维护者。
