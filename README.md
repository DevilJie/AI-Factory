<div align="center">

# AI Factory

**AI-Powered Novel Creation System**

AI 驱动的智能小说创作系统

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring_Boot-3.2.0-green.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.5-brightgreen.svg)](https://vuejs.org/)
[![Vite 7](https://img.shields.io/badge/Vite-7.2-646CFF.svg)](https://vitejs.dev/)

[English](#english) | [中文](#中文)

> **Online Demo:** https://ai.cjxch.com
> Demo Account: `a314170122` / `111111`

</div>

---

<a id="english"></a>

## English

### Project Introduction

**AI Factory** is a full-stack, AI-powered novel creation system designed to assist authors throughout the entire creative writing process. By integrating multiple Large Language Model (LLM) providers, it provides intelligent capabilities for world-building, character management, plot planning, chapter generation, foreshadowing tracking, and storyboard creation.

Whether you are writing web novels, literary fiction, or serialized stories, AI Factory acts as your intelligent co-pilot — helping you maintain consistency across complex narrative structures while significantly boosting creative productivity.

#### Key Features

- **AI-Assisted Outline Generation** — Automatically generate structured novel outlines with volume and chapter breakdowns based on your story concept, genre, and target length
- **Intelligent Chapter Content Generation** — Generate high-quality chapter content with word count control, plot consistency, and character voice maintenance
- **World-Building Management** — Create, organize, and reference rich world settings (geography, culture, magic systems, etc.) that AI can leverage during content generation
- **Character Profile System** — Maintain detailed character profiles with relationships, personality traits, and development arcs that ensure character consistency
- **Foreshadowing (Plant & Payoff) Tracking** — Track planted plot devices across chapters and ensure they are resolved at the right moment
- **Multi-LLM Provider Support** — Plug-and-play architecture supporting DeepSeek, OpenAI, ZhipuAI, and more through a unified provider interface
- **Async Task System** — Long-running AI operations (outline generation, chapter writing) run asynchronously with step-by-step progress tracking
- **Storyboard Creator** — Convert novel chapters into visual storyboard scripts for AI-powered image and video generation
- **Docker Deployment** — One-command deployment with Docker Compose for production environments

### Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Backend Language** | Java | 21 |
| **Backend Framework** | Spring Boot | 3.2.0 |
| **ORM** | MyBatis-Plus | 3.5.5 |
| **Database** | MySQL | 8.0+ |
| **Cache** | Redis | — |
| **AI Orchestration** | LangChain4j | 1.11.0 |
| **Authentication** | JWT (jjwt) | 0.12.3 |
| **API Documentation** | SpringDoc OpenAPI | 2.3.0 |
| **Frontend Framework** | Vue 3 | 3.5.x |
| **Frontend Language** | TypeScript | 5.9.x |
| **Build Tool** | Vite | 7.2.x |
| **State Management** | Pinia | 3.0.x |
| **CSS Framework** | Tailwind CSS | 4.1.x |
| **HTTP Client** | Axios | 1.13.x |
| **Icons** | Lucide Vue | 0.469.x |
| **Containerization** | Docker + Docker Compose | — |

### Project Structure

```
AI-Factory/
├── ai-factory-backend/              # Spring Boot backend
│   ├── src/main/java/com/aifactory/
│   │   ├── controller/              # REST API endpoints
│   │   ├── service/                 # Business logic
│   │   │   ├── llm/                 # LLM provider implementations
│   │   │   │                        # (DeepSeek, OpenAI, ZhipuAI)
│   │   │   ├── task/                # Async task strategies
│   │   │   │                        # (chapter generation, outline, etc.)
│   │   │   └── impl/                # Service implementations
│   │   ├── mapper/                  # MyBatis-Plus data mappers
│   │   ├── entity/                  # Database entity classes
│   │   ├── dto/                     # Data transfer objects
│   │   ├── common/                  # Utilities
│   │   │                           # (TokenUtil, PasswordUtil, UserContext)
│   │   └── common/xml/             # XML DTOs for LLM response parsing
│   │   └── config/                  # Spring configuration classes
│   └── src/main/resources/
│       ├── application.yml          # Main configuration
│       ├── application-dev.yml      # Development profile
│       ├── application-prod.yml     # Production profile
│       ├── mapper/                  # MyBatis XML mappers
│       └── db/                      # SQL init scripts
│
├── ai-factory-frontend2/            # Vue 3 frontend
│   └── src/
│       ├── api/                     # API client modules
│       ├── views/                   # Page components
│       │   ├── Novel/               # Novel creation module
│       │   ├── Project/             # Project management
│       │   ├── Login/               # Authentication
│       │   └── Settings/            # System settings
│       ├── components/              # Reusable components
│       ├── stores/                  # Pinia state stores
│       ├── router/                  # Vue Router configuration
│       ├── utils/                   # Utility functions
│       └── App.vue                  # Root component
│
├── docs/                            # Documentation
│   └── features/                    # Feature-specific documents
│
├── .deploy/                         # Deployment configuration
│   ├── Dockerfile                   # Single-container Dockerfile
│   ├── docker-compose.yml           # Docker Compose configuration
│   ├── nginx/                       # Nginx configuration
│   ├── supervisor/                  # Supervisor configuration
│   └── scripts/                     # Deployment scripts
│
├── LICENSE                          # MIT License
└── README.md                        # This file
```

### Prerequisites

Before getting started, make sure you have the following installed:

| Requirement | Minimum Version | Notes |
|-------------|----------------|-------|
| **Java JDK** | 21+ | Required for backend build and runtime |
| **Maven** | 3.8+ | Backend build tool |
| **Node.js** | 18+ | Frontend build tool |
| **npm** | 9+ | Package manager |
| **MySQL** | 8.0+ | Primary database |
| **Redis** | 6.0+ | Caching layer |
| **Docker** (optional) | 20+ | For containerized deployment |
| **Docker Compose** (optional) | 2.0+ | For containerized deployment |

### Quick Start

#### 1. Clone the Repository

```bash
git clone https://github.com/your-username/AI-Factory.git
cd AI-Factory
```

#### 2. Database Setup

Create the MySQL database and run the initialization scripts:

```sql
-- Create database
CREATE DATABASE ai_factory DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Import init scripts (located in ai-factory-backend/src/main/resources/db/)
-- Execute SQL files in order:
--   1. init.sql (table structure)
--   2. init_prompt_templates.sql (AI prompt templates)
```

#### 3. Backend Configuration

Edit `ai-factory-backend/src/main/resources/application-dev.yml` to match your environment:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_factory?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
```

#### 4. Start Backend

```bash
cd ai-factory-backend

# Build the project
mvn clean package -DskipTests

# Run in development mode
mvn spring-boot:run
```

The backend will start on **http://localhost:1024**.

> API documentation (Swagger UI) is available at: **http://localhost:1024/swagger-ui.html**

#### 5. Start Frontend

```bash
cd ai-factory-frontend2

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will start on **http://localhost:5174**.

The frontend dev server automatically proxies `/api/*` requests to the backend at `http://127.0.0.1:1024`.

### API Overview

The backend REST API runs on port **1024**. Key endpoint groups:

| Endpoint Group | Path Prefix | Description |
|---------------|-------------|-------------|
| **Authentication** | `/api/user/*` | Login, register, captcha |
| **Projects** | `/api/projects/*` | Project CRUD operations |
| **Novel** | `/api/novel/{projectId}/*` | Novel-specific operations |
| **Chapters** | `/api/chapters/*` | Chapter management |
| **Characters** | `/api/novel/{projectId}/characters/*` | Character profiles |
| **Worldview** | `/api/novel/{projectId}/worldview/*` | World settings |
| **Tasks** | `/api/tasks/*` | Async AI task management |
| **AI Providers** | `/api/ai-provider/*` | LLM provider configuration |
| **Prompt Templates** | `/api/prompt-template/*` | AI prompt template management |

### Production Deployment with Docker

The project provides a single-container deployment solution that bundles both the frontend (served by Nginx) and backend (Spring Boot) into one Docker image, managed by Supervisor.

#### Deploy with Docker Compose

```bash
# Navigate to the deployment directory
cd .deploy/remote

# Build and start the container
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop the container
docker-compose down
```

After deployment:
- **Frontend**: http://your-server:8084
- **Backend API**: http://your-server:1024 (or through Nginx proxy at http://your-server:8084/api/)

#### Container Architecture

```
┌─────────────────────────────────────────┐
│           Docker Container              │
│                                         │
│  ┌──────────┐      ┌──────────────────┐ │
│  │  Nginx   │─────▶│  Spring Boot     │ │
│  │  (Port 80)│      │  (Port 1024)     │ │
│  │          │      │                  │ │
│  │ Frontend │      │   Backend API    │ │
│  │ Static   │      │                  │ │
│  │ Files    │      └──────────────────┘ │
│  └──────────┘                           │
│         ↑                               │
│     Supervisor                          │
└─────────────────────────────────────────┘
```

#### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring Boot profile |
| `TZ` | `Asia/Shanghai` | Container timezone |
| `JAVA_OPTS` | `-Xms512m -Xmx1024m` | JVM memory settings |

### Architecture Highlights

#### LLM Provider Pattern

The backend uses a factory pattern for multi-LLM support. Each provider implements a unified `LLMProvider` interface:

```java
// Unified interface for all LLM providers
LLMProvider provider = llmProviderFactory.getProvider("deepseek");
AIGenerateResponse response = provider.generate(request);
```

Supported providers: **DeepSeek**, **OpenAI**, **ZhipuAI** — easily extensible to add new providers.

#### Async Task System

Long-running AI operations use a strategy pattern with step-by-step progress tracking:

```
TaskStrategy Interface
├── OutlineTaskStrategy              — Novel outline generation
├── ChapterGenerationTaskStrategy    — Chapter content writing
├── VolumeOptimizeTaskStrategy       — Volume-level optimization
├── WorldviewTaskStrategy            — World-building generation
└── ChapterFixTaskStrategy           — Chapter revision & fixing
```

Each task consists of multiple steps with status tracking: `pending` → `running` → `completed` / `failed`.

#### XML-Based LLM Response Parsing

The system uses Jackson XML for parsing structured LLM responses with minimal token usage:

```java
@Autowired
private XmlParser xmlParser;

// Parse XML response to POJO
ChapterMemoryXmlDto dto = xmlParser.parse(xmlString, ChapterMemoryXmlDto.class);
```

### License

This project is licensed under the [MIT License](LICENSE).

---

<a id="中文"></a>

## 中文

### 项目简介

**AI Factory** 是一个全栈的 AI 驱动智能小说创作系统，旨在辅助作者完成从构思到成稿的整个创作流程。系统集成了多家大语言模型（LLM）服务商，提供世界观构建、人物管理、情节规划、章节生成、伏笔追踪、分镜创作等智能化创作能力。

无论您是创作网络小说、文学作品还是连载故事，AI Factory 都可以作为您的智能创作助手——帮助您在复杂的叙事结构中保持一致性，显著提升创作效率。

#### 核心功能

- **AI 辅助大纲生成** — 根据故事概念、类型和目标篇幅，自动生成包含分卷、分章的结构化小说大纲
- **智能章节内容生成** — 生成高质量章节内容，支持字数控制、情节连贯性和人物语气一致性
- **世界观管理** — 创建、组织和引用丰富的世界设定（地理、文化、魔法体系等），供 AI 在内容生成时参考
- **人物档案系统** — 维护详细的人物档案，包括人物关系、性格特点和发展弧线，确保人物一致性
- **伏笔（埋线与回收）追踪** — 跨章节追踪已埋设的情节伏笔，确保在合适的时机回收
- **多 LLM 服务商支持** — 插件化架构，支持 DeepSeek、OpenAI、智谱AI 等多家服务商，统一接口调用
- **异步任务系统** — 长时间运行的 AI 操作（大纲生成、章节写作）异步执行，支持步骤级进度追踪
- **分镜创作器** — 将小说章节转换为可视化分镜脚本，用于 AI 生图和视频制作
- **Docker 一键部署** — 使用 Docker Compose 一键完成生产环境部署

### 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端语言** | Java | 21 |
| **后端框架** | Spring Boot | 3.2.0 |
| **ORM 框架** | MyBatis-Plus | 3.5.5 |
| **数据库** | MySQL | 8.0+ |
| **缓存** | Redis | — |
| **AI 编排** | LangChain4j | 1.11.0 |
| **认证方式** | JWT (jjwt) | 0.12.3 |
| **API 文档** | SpringDoc OpenAPI | 2.3.0 |
| **前端框架** | Vue 3 | 3.5.x |
| **前端语言** | TypeScript | 5.9.x |
| **构建工具** | Vite | 7.2.x |
| **状态管理** | Pinia | 3.0.x |
| **CSS 框架** | Tailwind CSS | 4.1.x |
| **HTTP 客户端** | Axios | 1.13.x |
| **图标库** | Lucide Vue | 0.469.x |
| **容器化** | Docker + Docker Compose | — |

### 项目结构

```
AI-Factory/
├── ai-factory-backend/              # Spring Boot 后端
│   ├── src/main/java/com/aifactory/
│   │   ├── controller/              # REST API 接口
│   │   ├── service/                 # 业务逻辑层
│   │   │   ├── llm/                 # LLM 服务商实现
│   │   │   │                        # （DeepSeek、OpenAI、智谱AI）
│   │   │   ├── task/                # 异步任务策略
│   │   │   │                        # （章节生成、大纲生成等）
│   │   │   └── impl/                # 服务实现类
│   │   ├── mapper/                  # MyBatis-Plus 数据映射器
│   │   ├── entity/                  # 数据库实体类
│   │   ├── dto/                     # 数据传输对象
│   │   ├── common/                  # 工具类
│   │   │                           # （TokenUtil、PasswordUtil、UserContext）
│   │   ├── common/xml/             # XML DTO（LLM 响应解析）
│   │   └── config/                  # Spring 配置类
│   └── src/main/resources/
│       ├── application.yml          # 主配置文件
│       ├── application-dev.yml      # 开发环境配置
│       ├── application-prod.yml     # 生产环境配置
│       ├── mapper/                  # MyBatis XML 映射文件
│       └── db/                      # SQL 初始化脚本
│
├── ai-factory-frontend2/            # Vue 3 前端
│   └── src/
│       ├── api/                     # API 请求模块
│       ├── views/                   # 页面组件
│       │   ├── Novel/               # 小说创作模块
│       │   ├── Project/             # 项目管理
│       │   ├── Login/               # 登录认证
│       │   └── Settings/            # 系统设置
│       ├── components/              # 可复用组件
│       ├── stores/                  # Pinia 状态管理
│       ├── router/                  # Vue Router 路由配置
│       ├── utils/                   # 工具函数
│       └── App.vue                  # 根组件
│
├── docs/                            # 项目文档
│   └── features/                    # 功能特性文档
│
├── .deploy/                         # 部署配置
│   ├── Dockerfile                   # 单容器 Dockerfile
│   ├── docker-compose.yml           # Docker Compose 配置
│   ├── nginx/                       # Nginx 配置
│   ├── supervisor/                  # Supervisor 配置
│   └── scripts/                     # 部署脚本
│
├── LICENSE                          # MIT 开源协议
└── README.md                        # 本文件
```

### 环境要求

在开始之前，请确保已安装以下软件：

| 要求 | 最低版本 | 说明 |
|------|---------|------|
| **Java JDK** | 21+ | 后端编译和运行所需 |
| **Maven** | 3.8+ | 后端构建工具 |
| **Node.js** | 18+ | 前端构建工具 |
| **npm** | 9+ | 包管理器 |
| **MySQL** | 8.0+ | 主数据库 |
| **Redis** | 6.0+ | 缓存服务 |
| **Docker**（可选） | 20+ | 容器化部署 |
| **Docker Compose**（可选） | 2.0+ | 容器编排 |

### 快速开始

#### 1. 克隆项目

```bash
git clone https://github.com/your-username/AI-Factory.git
cd AI-Factory
```

#### 2. 数据库初始化

创建 MySQL 数据库并执行初始化脚本：

```sql
-- 创建数据库
CREATE DATABASE ai_factory DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入初始化脚本（位于 ai-factory-backend/src/main/resources/db/）
-- 按顺序执行：
--   1. init.sql（表结构）
--   2. init_prompt_templates.sql（AI 提示词模板）
```

#### 3. 后端配置

编辑 `ai-factory-backend/src/main/resources/application-dev.yml`，修改为您的环境配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_factory?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: 您的用户名
    password: 您的密码

  data:
    redis:
      host: localhost
      port: 6379
      password: 您的Redis密码
```

#### 4. 启动后端

```bash
cd ai-factory-backend

# 构建项目
mvn clean package -DskipTests

# 开发模式启动
mvn spring-boot:run
```

后端服务将在 **http://localhost:1024** 启动。

> API 接口文档（Swagger UI）访问地址：**http://localhost:1024/swagger-ui.html**

#### 5. 启动前端

```bash
cd ai-factory-frontend2

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 **http://localhost:5174** 启动。

前端开发服务器会自动将 `/api/*` 请求代理到后端 `http://127.0.0.1:1024`。

### 接口概览

后端 REST API 运行在 **1024** 端口，主要接口分组如下：

| 接口分组 | 路径前缀 | 说明 |
|---------|---------|------|
| **用户认证** | `/api/user/*` | 登录、注册、验证码 |
| **项目管理** | `/api/projects/*` | 项目增删改查 |
| **小说操作** | `/api/novel/{projectId}/*` | 小说相关操作 |
| **章节管理** | `/api/chapters/*` | 章节内容管理 |
| **人物管理** | `/api/novel/{projectId}/characters/*` | 人物档案 |
| **世界观** | `/api/novel/{projectId}/worldview/*` | 世界设定 |
| **任务管理** | `/api/tasks/*` | 异步 AI 任务 |
| **AI 服务商** | `/api/ai-provider/*` | LLM 服务商配置 |
| **提示词模板** | `/api/prompt-template/*` | AI 提示词模板管理 |

### Docker 生产部署

项目提供单容器部署方案，将前端（Nginx 托管）和后端（Spring Boot）打包到一个 Docker 镜像中，由 Supervisor 统一管理。

#### 使用 Docker Compose 部署

```bash
# 进入部署目录
cd .deploy/remote

# 构建并启动容器
docker-compose up -d --build

# 查看日志
docker-compose logs -f

# 停止容器
docker-compose down
```

部署完成后：
- **前端访问**：http://your-server:8084
- **后端接口**：http://your-server:1024（或通过 Nginx 代理访问 http://your-server:8084/api/）

#### 容器架构

```
┌─────────────────────────────────────────┐
│           Docker 容器                   │
│                                         │
│  ┌──────────┐      ┌──────────────────┐ │
│  │  Nginx   │─────▶│  Spring Boot     │ │
│  │  (端口 80)│      │  (端口 1024)     │ │
│  │          │      │                  │ │
│  │ 前端静态  │      │   后端 API       │ │
│  │ 资源文件  │      │                  │ │
│  └──────────┘      └──────────────────┘ │
│         ↑                               │
│     Supervisor 进程管理                   │
└─────────────────────────────────────────┘
```

#### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|-------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring Boot 运行环境 |
| `TZ` | `Asia/Shanghai` | 容器时区 |
| `JAVA_OPTS` | `-Xms512m -Xmx1024m` | JVM 内存参数 |

### 架构亮点

#### LLM 服务商模式

后端使用工厂模式实现多 LLM 服务商支持，每个服务商实现统一的 `LLMProvider` 接口：

```java
// 统一接口调用任意 LLM 服务商
LLMProvider provider = llmProviderFactory.getProvider("deepseek");
AIGenerateResponse response = provider.generate(request);
```

已支持的服务商：**DeepSeek**、**OpenAI**、**智谱AI** —— 可轻松扩展新的服务商。

#### 异步任务系统

长时间运行的 AI 操作采用策略模式，支持步骤级进度追踪：

```
TaskStrategy 接口
├── OutlineTaskStrategy              — 小说大纲生成
├── ChapterGenerationTaskStrategy    — 章节内容写作
├── VolumeOptimizeTaskStrategy       — 分卷级优化
├── WorldviewTaskStrategy            — 世界观生成
└── ChapterFixTaskStrategy           — 章节修订与修正
```

每个任务由多个步骤组成，状态流转：`pending`（待处理）→ `running`（执行中）→ `completed`（已完成）/ `failed`（失败）。

#### XML 格式 LLM 响应解析

系统使用 Jackson XML 解析结构化 LLM 响应，以最小化 Token 消耗：

```java
@Autowired
private XmlParser xmlParser;

// 将 XML 响应解析为 Java 对象
ChapterMemoryXmlDto dto = xmlParser.parse(xmlString, ChapterMemoryXmlDto.class);
```

### 开源协议

本项目基于 [MIT 开源协议](LICENSE) 发布。

---

<div align="center">

**AI Factory** — Let AI empower your creative writing journey.

AI Factory — 让 AI 赋能你的创作之旅。

</div>
