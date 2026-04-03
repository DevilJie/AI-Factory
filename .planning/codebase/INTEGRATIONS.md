# External Integrations

**Analysis Date:** 2026-04-01

## APIs & External Services

**AI/LLM Providers:**
- DeepSeek - AI content generation provider
  - SDK: Custom implementation via LangChain4j
  - Auth: API key in AiProvider configuration
- OpenAI - AI content generation provider
  - SDK: langchain4j-open-ai 1.11.0
  - Auth: API key in AiProvider configuration
- ZhipuAI (智谱AI) - AI content generation provider
  - SDK: Custom implementation via LangChain4j
  - Auth: API key in AiProvider configuration

**Authentication & Identity:**
- JWT (jjwt) - Token-based authentication
  - Implementation: Custom JWT utilities
  - Storage: Redis for token management

## Data Storage

**Databases:**
- MySQL 8.0+ - Primary database
  - Connection: JDBC via MyBatis-Plus
  - ORM: MyBatis-Plus 3.5.5
  - Entity mapping: com.aifactory.entity package
- SQLite 3.44.1.0 - Alternative database support
  - Connection: JDBC connector
  - Usage: Development/testing scenarios

**File Storage:**
- Local filesystem only - Static files and uploads

**Caching:**
- Redis 6.0+ - Caching layer
  - Client: Jedis with connection pooling
  - Configuration: Spring Boot Data Redis
  - Usage: Session storage, caching, rate limiting

## Authentication & Identity

**Auth Provider:**
- Custom JWT implementation
  - Implementation: TokenUtil, PasswordUtil
  - Endpoints: /api/user/* (login, register, captcha)
  - Storage: Redis for token blacklisting

## Monitoring & Observability

**Error Tracking:**
- Custom logging - Spring Boot logging framework
- Exception handling: GlobalExceptionHandler

**Logs:**
- Application logs - Spring Boot logging
- Structured logging: Log levels configurable per environment
- File location: logs/ directory

## CI/CD & Deployment

**Hosting:**
- Local development - Direct Java/Node.js execution
- Production - Docker containerization
  - Container: Single-container with Nginx + Spring Boot
  - Orchestration: Docker Compose

**CI Pipeline:**
- Maven build - Backend compilation and packaging
- Vite build - Frontend asset compilation
- Docker image creation - Multi-stage builds

## Environment Configuration

**Required env vars:**
- Database connection (MySQL)
  - SPRING_DATASOURCE_URL
  - SPRING_DATASOURCE_USERNAME
  - SPRING_DATASOURCE_PASSWORD
- Redis connection
  - SPRING_DATA_REDIS_HOST
  - SPRING_DATA_REDIS_PORT
  - SPRING_DATA_REDIS_PASSWORD
- AI provider API keys
  - DEEPSEEK_API_KEY
  - OPENAI_API_KEY
  - ZHIPU_API_KEY

**Secrets location:**
- Configuration files: application-dev.yml, application-prod.yml
- Environment variables for sensitive data
- Encrypted storage recommended for production

## Webhooks & Callbacks

**Incoming:**
- None detected - No webhook endpoints in codebase

**Outgoing:**
- None detected - No external webhook calls

---

*Integration audit: 2026-04-01*