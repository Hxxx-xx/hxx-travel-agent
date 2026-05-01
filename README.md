# hxx-travel-agent

旅行行程规划 Java 后端服务

## 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 3.2 | Web 框架 |
| MyBatis-Plus 3.5 | ORM 框架 |
| Druid | 数据库连接池 |
| MySQL 8.0 | 关系数据库 |
| Redis | 缓存 |
| LangChain4j | AI 调用 |
| Qdrant | 向量数据库 |
| Sa-Token | 认证（预留） |
| OpenPDF | PDF 导出 |

## 项目结构

```
hxx-travel-agent/
├── pom.xml                          # 父 POM
├── hxx-travel-agent-common/          # 通用模块（异常、工具类）
├── hxx-travel-agent-dto/             # 数据传输对象
├── hxx-travel-agent-entity/          # 数据库实体
├── hxx-travel-agent-rag/             # RAG 向量检索
├── hxx-travel-agent-agent/           # Agent LLM 调用
├── hxx-travel-agent-service/         # 业务服务层
└── hxx-travel-agent-api/            # Web Controller 层
```

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

## 快速开始

### 1. 创建数据库

```sql
-- 创建开发环境数据库
CREATE DATABASE hxx_travel_agent_dev
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 创建测试环境数据库
CREATE DATABASE hxx_travel_agent_test
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 创建生产环境数据库
CREATE DATABASE hxx_travel_agent
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 执行建表脚本
USE hxx_travel_agent_dev;
SOURCE hxx-travel-agent-entity/src/main/resources/sql/schema.sql;
```

### 2. 配置环境变量（可选）

```bash
# LLM 配置
export LLM_API_KEY=your-api-key
export LLM_BASE_URL=your-base-url
export LLM_MODEL=your-model

# 高德地图配置
export AMAP_API_KEY=your-amap-key

# 生产环境数据库配置
export DB_HOST=your-mysql-host
export DB_PORT=3306
export DB_NAME=hxx_travel_agent
export DB_USERNAME=root
export DB_PASSWORD=your-password

# 生产环境 Redis 配置
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password
```

### 3. 编译运行

```bash
# 编译项目
mvn clean install

# 运行开发环境
mvn spring-boot:run -pl hxx-travel-agent-api

# 指定环境运行
mvn spring-boot:run -pl hxx-travel-agent-api -Dspring.profiles.active=prod
```

### 4. 访问服务

- API 文档：http://localhost:8080/swagger-ui.html
- Druid 监控（开发环境）：http://localhost:8080/druid/
  - 用户名：admin
  - 密码：admin

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /trip/generate | 生成行程 |
| POST | /trip/edit | 编辑行程 |
| POST | /trip/save | 保存行程 |
| GET | /trip | 行程列表 |
| GET | /trip/{tripId} | 行程详情 |
| DELETE | /trip/{tripId} | 删除行程 |
| GET | /weather/forecast | 天气预报 |
| GET | /export/{tripId}/markdown | 导出 Markdown |
| GET | /export/{tripId}/pdf | 导出 PDF |

## 环境配置

| 环境 | 配置文件 | 数据库 |
|------|---------|--------|
| 开发 | application-dev.yml | hxx_travel_agent_dev |
| 测试 | application-test.yml | hxx_travel_agent_test |
| 生产 | application-prod.yml | hxx_travel_agent |

## 目录说明

- `data/` - RAG 攻略数据文件
- `logs/` - 日志文件（生产环境）
- `schema.sql` - 数据库建表脚本
