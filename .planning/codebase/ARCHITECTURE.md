# Architecture

**Analysis Date:** 2026-04-01

## Pattern Overview

**Overall:** Layered Spring Boot Architecture with Domain-Driven Design Elements

**Key Characteristics:**
- Spring Boot 3.2.0 with Java 21
- RESTful API design with Swagger documentation
- MyBatis-Plus for database operations with SQLite
- LangChain4j for AI/LLM integration
- Event-driven architecture with SSE for real-time updates
- Template-based prompt engineering system
- Modular chapter processing pipeline

## Layers

**Controller Layer:**
- Purpose: HTTP request handling, API endpoint definitions
- Location: `ai-factory-backend/src/main/java/com/aifactory/controller`
- Contains: REST controllers for business domains
- Depends on: Service layer
- Used by: HTTP clients, Swagger UI

**Service Layer:**
- Purpose: Business logic orchestration, cross-cutting concerns
- Location: `ai-factory-backend/src/main/java/com/aifactory/service`
- Contains: Service interfaces and implementations
- Depends on: Mapper layer, domain entities
- Used by: Controller layer
- Key patterns: Transaction management, caching, AOP

**Domain Layer:**
- Purpose: Business entities and data structures
- Location: `ai-factory-backend/src/main/java/com/aifactory/entity`
- Contains: JPA entities with MyBatis-Plus annotations
- Depends on: None (pure domain objects)
- Used by: All layers

**Mapper Layer:**
- Purpose: Database access and persistence
- Location: `ai-factory-backend/src/main/java/com/aifactory/mapper`
- Contains: MyBatis mapper interfaces
- Depends on: Database driver
- Used by: Service layer

**Infrastructure Layer:**
- Purpose: External service integrations, configurations
- Location: `ai-factory-backend/src/main/java/com/aifactory/config`, `ai-factory-backend/src/main/java/com/aifactory/service/llm`
- Contains: Database configs, JWT filters, AI provider integrations
- Depends on: Third-party libraries
- Used by: Service layer

## Data Flow

**Chapter Generation Flow:**

1. Controller receives HTTP request (ChapterController)
2. Service layer orchestrates (ChapterService)
3. PromptTemplateBuilder constructs AI prompts
4. LLMProvider calls external AI services
5. StreamingChatHelper processes real-time responses
6. ChapterPersistenceService saves to database
7. Returns SSE updates to client

**AI Integration Flow:**

1. AIGenerateRequest created with context
2. LLMProviderFactory selects appropriate AI provider
3. generateStream() called with callback
4. Tokens streamed back via StreamCallback
5. Response processed and formatted
6. AiInteractionLog captures all interactions

**State Management:**
- Spring-managed beans for singletons
- Database state via MyBatis-Plus
- Redis caching for prompt templates (disabled in dev)
- User context via JWT tokens
- Transaction boundaries at service layer

## Key Abstractions

**LLMProvider:**
- Purpose: Abstract AI provider interface
- Examples: `[ai-factory-backend/src/main/java/com/aifactory/service/llm/LLMProvider.java]`
- Pattern: Strategy pattern for multiple AI providers

**Chapter Processing Pipeline:**
- Purpose: Modular chapter generation components
- Examples: `[ai-factory-backend/src/main/java/com/aifactory/service/chapter/persistence/]`, `[ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/]`
- Pattern: Pipeline with separate concerns (persistence, parsing, prompting)

**Prompt Template System:**
- Purpose: Template-based prompt engineering
- Examples: `[ai-factory-backend/src/main/java/com/aifactory/service/prompt/PromptTemplateService.java]`
- Pattern: Template engine with dynamic variable substitution

**Entity Domain Model:**
- Purpose: Business objects with database mapping
- Examples: `[ai-factory-backend/src/main/java/com/aifactory/entity/Chapter.java]`, `[ai-factory-backend/src/main/java/com/aifactory/entity/NovelWorldview.java]`
- Pattern: Active Record pattern with MyBatis-Plus

## Entry Points

**Application Entry Point:**
- Location: `[ai-factory-backend/src/main/java/com/aifactory/AiFactoryBackendApplication.java]`
- Triggers: Spring Boot startup
- Responsibilities: Component scanning, mapper scanning, application startup

**HTTP Entry Points:**
- Location: `ai-factory-backend/src/main/java/com/aifactory/controller/*Controller.java`
- Triggers: HTTP requests
- Responsibilities: Request validation, response formatting, API versioning

**AI Service Entry Point:**
- Location: `ai-factory-backend/src/main/java/com/aifactory/service/AIGenerateService.java`
- Triggers: Chapter generation requests
- Responsibilities: Context assembly, AI provider selection, streaming coordination

## Error Handling

**Strategy:** Centralized exception handling with global handler

**Patterns:**
- GlobalExceptionHandler for uncaught exceptions
- Custom exception types for business errors
- Result<T> wrapper for consistent API responses
- Transaction rollback for data consistency

## Cross-Cutting Concerns

**Logging:** SLF4J with structured logging patterns
- Async logging performance
- Request/response tracing
- AI interaction audit logging

**Validation:** Spring Validation with custom validators
- DTO validation on request boundaries
- Business rule validation in services
- Database constraint validation

**Authentication:** JWT-based authentication
- Custom JwtAuthenticationFilter
- User context propagation
- Role-based access control patterns

---

*Architecture analysis: 2026-04-01*