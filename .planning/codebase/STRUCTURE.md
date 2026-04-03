# Codebase Structure

**Analysis Date:** 2026-04-01

## Directory Layout

```
AI-Factory/
├── .claude/                    # Claude agent configuration
│   ├── agents/                 # Agent implementations
│   ├── commands/               # CLI commands
│   ├── get-shit-done/          # GSD framework
│   ├── hooks/                  # Git hooks
│   └── references/             # Reference materials
├── .idea/                     # IDE configuration
├── .planning/                 # Planning documents
│   └── codebase/              # Architecture analysis
├── ai-factory-backend/        # Spring Boot backend
│   ├── deploy/                # Deployment artifacts
│   ├── logs/                  # Application logs
│   ├── pom.xml                # Maven dependencies
│   └── src/main/
│       ├── java/com/aifactory/
│       │   ├── AiFactoryBackendApplication.java  # Main application
│       │   ├── aspect/                         # AOP cross-cutting concerns
│       │   ├── common/                         # Utility classes
│       │   │   ├── xml/                         # XML parsing utilities
│       │   │   └── ...                          # Shared utilities (captcha, password, token)
│       │   ├── config/                         # Configuration classes
│       │   │   ├── AsyncConfig.java             # Async task configuration
│       │   │   ├── CacheConfig.java            # Cache configuration
│       │   │   ├── CorsConfig.java             # CORS settings
│       │   │   ├── DatabaseInitConfig.java     # Database initialization
│       │   │   ├── GlobalExceptionHandler.java  # Global error handling
│       │   │   ├── JacksonConfig.java           # JSON serialization
│       │   │   ├── JwtAuthenticationFilter.java # JWT auth filter
│       │   │   ├── MybatisPlusConfig.java      # MyBatis config
│       │   │   ├── SwaggerConfig.java          # API documentation
│       │   │   └── TemplateCacheInitializer.java # Template caching
│       │   ├── constants/                      # Application constants
│       │   ├── controller/                     # REST API controllers
│       │   ├── dto/                           # Data Transfer Objects
│       │   ├── entity/                        # JPA entities
│       │   ├── enums/                         # Enumeration definitions
│       │   ├── exception/                     # Custom exceptions
│       │   ├── mapper/                        # MyBatis mappers
│       │   ├── response/                       # API response wrappers
│       │   ├── service/                       # Business service layer
│       │   │   ├── impl/                      # Service implementations
│       │   │   ├── chapter/                   # Chapter-specific services
│       │   │   │   ├── parser/               # Chapter content parsing
│       │   │   │   ├── persistence/          # Chapter data persistence
│       │   │   │   └── prompt/               # Chapter prompt building
│       │   │   ├── llm/                      # AI/LLM integration
│       │   │   └── prompt/                   # Prompt template services
│       │   └── ...                            # Other services
│       ├── resources/
│       │   ├── application*.yml               # Spring config files
│       │   ├── mapper/                        # MyBatis XML mappers
│       │   └── static/                        # Static resources
│       └── test/                              # Test files
└── deploy/                   # Production deployment scripts
```

## Directory Purposes

**ai-factory-backend/src/main/java/com/aifactory:**
- Purpose: Main application package
- Contains: All application components
- Key files: Entry point, configurations, business logic

**controller/:**
- Purpose: HTTP request handling and API definitions
- Contains: REST controllers for each domain
- Key files: ChapterController.java, AIController.java

**service/:**
- Purpose: Business logic layer
- Contains: Service interfaces and implementations
- Key files: ChapterService.java, AIGenerateService.java

**entity/:**
- Purpose: Domain models and database entities
- Contains: JPA entities with annotations
- Key files: Chapter.java, NovelWorldview.java

**mapper/:**
- Purpose: Database access layer
- Contains: MyBatis mapper interfaces
- Key files: ChapterMapper.java, NovelCharacterMapper.java

**config/:**
- Purpose: Application configuration
- Contains: Spring configuration classes
- Key files: MybatisPlusConfig.java, GlobalExceptionHandler.java

**common/:**
- Purpose: Shared utilities and helpers
- Contains: Reusable components
- Key files: UserContext.java, XmlParser.java

**service/chapter/:**
- Purpose: Chapter processing specific logic
- Contains: Specialized chapter services
- Sub-directories: parser, persistence, prompt

**service/llm/:**
- Purpose: AI/LLM integration
- Contains: AI provider implementations
- Key files: LLMProvider.java, LLMProviderFactory.java

## Key File Locations

**Entry Points:**
- `[ai-factory-backend/src/main/java/com/aifactory/AiFactoryBackendApplication.java]`: Spring Boot entry point
- `[ai-factory-backend/src/main/java/com/aifactory/controller/ChapterController.java]`: Chapter management API
- `[ai-factory-backend/src/main/java/com/aifactory/controller/AIController.java]`: AI generation API

**Configuration:**
- `[ai-factory-backend/pom.xml]`: Maven dependencies and build configuration
- `[ai-factory-backend/src/main/resources/application.yml]`: Spring application configuration
- `[ai-factory-backend/src/main/resources/application-dev.yml]`: Development environment config

**Core Logic:**
- `[ai-factory-backend/src/main/java/com/aifactory/service/ChapterService.java]`: Chapter business logic
- `[ai-factory-backend/src/main/java/com/aifactory/service/llm/LLMProviderFactory.java]`: AI provider selection
- `[ai-factory-backend/src/main/java/com/aifactory/service/chapter/persistence/ChapterPersistenceService.java]`: Chapter data persistence

**Testing:**
- `[ai-factory-backend/src/test/]`: Unit and integration tests
- No test files detected in current structure

## Naming Conventions

**Files:**
- Controllers: `*Controller.java` (e.g., ChapterController.java)
- Services: `*Service.java` for interfaces, `*ServiceImpl.java` for implementations
- Entities: `*.java` with descriptive names (e.g., NovelCharacter.java)
- Mappers: `*Mapper.java`
- DTOs: `*Dto.java` or *Request/*Response.java
- Configurations: `*Config.java`
- Enums: `*.java` in enums package

**Packages:**
- Lowercase with underscores for directories
- Domain-specific sub-packages (e.g., service.llm, service.chapter)
- Layer-based organization (controller, service, entity, mapper)

**Variables:**
- CamelCase for Java variables
- snake_case for SQL/MyBatis queries
- Constants in UPPER_SNAKE_CASE

## Where to Add New Code

**New Feature (Novel World Element):**
- Primary code: `ai-factory-backend/src/main/java/com/aifactory/service/impl/`
- Interface: `ai-factory-backend/src/main/java/com/aifactory/service/`
- Entity: `ai-factory-backend/src/main/java/com/aifactory/entity/`
- Mapper: `ai-factory-backend/src/main/java/com/aifactory/mapper/`
- Controller: `ai-factory-backend/src/main/java/com/aifactory/controller/`

**New AI Provider Integration:**
- Implementation: `ai-factory-backend/src/main/java/com/aifactory/service/llm/impl/`
- Interface: `ai-factory-backend/src/main/java/com/aifactory/service/llm/LLMProvider.java`
- Configuration: Add to application.yml

**New Chapter Processing Step:**
- Logic: `ai-factory-backend/src/main/java/com/aifactory/service/chapter/`
- Persistence: `ai-factory-backend/src/main/java/com/aifactory/service/chapter/persistence/`
- Prompts: `ai-factory-backend/src/main/java/com/aifactory/service/chapter/prompt/`

**Utilities:**
- Shared helpers: `ai-factory-backend/src/main/java/com/aifactory/common/`

## Special Directories

**.planning/codebase/:**
- Purpose: Architecture analysis documents
- Generated: Yes
- Committed: Yes

**ai-factory-backend/logs/:**
- Purpose: Application log files
- Generated: Yes
- Committed: No

**ai-factory-backend/deploy/:**
- Purpose: Production deployment artifacts
- Generated: Yes
- Committed: No

**ai-factory-backend/src/main/resources/mapper/:**
- Purpose: MyBatis XML mapping files
- Generated: No
- Committed: Yes

---

*Structure analysis: 2026-04-01*