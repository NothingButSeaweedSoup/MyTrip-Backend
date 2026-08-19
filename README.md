 # My_Trip 后端服务

 基于 **Spring Boot 3.5 + Java 21 + MyBatis-Plus + MySQL + Redis** 构建的智能旅游平台后端服务，提供用户认证、内容管理、AI 审核、智能搜索推荐、行程规划等完整 API 支持。

 ## 技术栈

 - **基础框架**: Spring Boot 3.5.14, Java 21, Maven
 - **持久层**: MyBatis-Plus 3.5.9, MySQL 8
 - **缓存**: Redis (Lettuce), Caffeine 本地缓存
 - **安全**: Spring Security, JWT (jjwt 0.12.6), BCrypt 密码加密
 - **AI 集成**: LangChain4j 1.13.1（DeepSeek 对话、SiliconFlow 向量嵌入、视觉模型）
 - **其他**: Spring AOP, Spring Mail, Spring Validation, Commons-Pool2

 ## 功能模块

 | 模块 | 说明 |
 |------|------|
 | **用户系统** | 注册、登录、个人信息管理、密码修改、角色权限（普通用户/审核员/管理员） |
 | **内容系统** | 游记发布、编辑、图片上传、标签管理、富文本支持 |
 | **评论系统** | 游记评论、回复、评论管理 |
 | **收藏系统** | 游记收藏与取消收藏 |
 | **AI 审核** | 集成 AI 对发布内容进行敏感词检测与审核，支持自动/人工审核流程 |
 | **搜索系统** | 混合搜索（关键词 + 语义向量），支持图文检索 |
 | **推荐系统** | 基于热度、标签、时效性、多样性加权排序的个性化推荐 |
 | **景点系统** | 景点 CRUD、景点标签关联、景点与游记关联 |
 | **行程规划** | 智能行程规划（基于 AI Agent 的多轮对话规划）、行程会话管理 |
 | **管理系统** | 仪表盘统计、用户管理、内容审核、配置管理、敏感词管理、邮件推送 |

 ## 快速启动

 ### 前置要求

 - JDK 21+
 - Maven 3.6+
 - MySQL 8.0+
 - Redis 6.0+

 ### 配置数据库

 创建数据库 `tourism`，执行 `sql/` 目录下的初始化脚本。

 ### 修改配置

 编辑 `src/main/resources/application.yml`，修改以下配置：

 - `spring.datasource` — 数据库连接信息
 - `spring.data.redis` — Redis 连接信息
 - `jwt.secret` — JWT 密钥（生产环境务必更换）
 - `upload.path` — 文件上传路径
 - `app.images-path` — 图片资源路径
 - `ai.*` — AI 服务 API Key（DeepSeek / SiliconFlow / 视觉模型）

 ### 启动服务

 ```bash
 # 开发模式
 mvn spring-boot:run

 # 或打包后运行
 mvn package -DskipTests
 java -jar target/backend-0.0.1-SNAPSHOT.jar
 ```

 服务默认运行在 **`http://localhost:8180`**。

 ## 项目结构

 ```
 backend/
 ├── src/main/java/com/backend/
 │   ├── annotation/         # 自定义注解（如 @RateLimit）
 │   ├── aspect/             # AOP 切面
 │   ├── common/             # 通用类（统一返回结果、异常处理、Redis键定义等）
 │   ├── config/             # 配置类（安全、Redis、缓存、AI、异步等）
 │   ├── controller/         # REST 控制器
 │   ├── dto/                # 数据传输对象（请求/响应）
 │   ├── entity/             # 数据库实体
 │   ├── mapper/             # MyBatis-Plus Mapper 接口
 │   ├── runner/             # 启动任务
 │   ├── security/           # JWT 认证过滤器
 │   ├── service/            # 服务接口与实现
 │   │   ├── impl/           # 服务实现
 │   │   └── ai/             # AI 相关服务
 │   └── task/               # 定时任务（计数同步、热度计算等）
 ├── src/main/resources/
 │   └── com/backend/mapper/ # MyBatis XML 映射文件
 ├── Dockerfile
 └── pom.xml
 ```

 ## Docker 部署

 项目根目录提供了多环境 Docker Compose 配置：

 ```bash
 # 启动全部服务（后端 + 前端 + MySQL + Redis）
 docker compose up -d
 ```
