# Technology Stack

**Analysis Date:** 2026-04-01

## Languages

**Primary:**
- Java 21 - Backend services and business logic
- TypeScript 5.9.x - Frontend application development

**Secondary:**
- SQL - Database queries and schema definition
- XML - Configuration and LLM response parsing

## Runtime

**Environment:**
- Java 21 (JDK) - Runtime environment
- Node.js 18+ - Frontend build and runtime

**Package Manager:**
- Maven 3.8+ - Backend dependency management
- npm 9+ - Frontend dependency management
- Lockfile: package-lock.json present

## Frameworks

**Core:**
- Spring Boot 3.2.0 - Backend framework
- Vue 3.5.x - Frontend framework
- Vite 7.2.x - Frontend build tool and development server

**Testing:**
- JUnit 5 - Unit testing framework
- Mockito - Mocking framework
- Spring Boot Test - Integration testing

**Build/Dev:**
- Maven - Backend build and dependency management
- Vite - Frontend build tooling
- TypeScript - Type checking and compilation
- Tailwind CSS 4.1.x - Utility-first CSS framework

## Key Dependencies

**Critical:**
- MyBatis-Plus 3.5.5 - ORM framework for database operations
- LangChain4j 1.11.0 - AI orchestration framework
- jjwt 0.12.3 - JWT token implementation
- Hutool 5.8.24 - Java utility library

**Infrastructure:**
- Spring Boot Web - REST API support
- Spring Boot Data Redis - Caching layer
- Spring Boot Validation - Input validation
- SpringDoc OpenAPI 2.3.0 - API documentation
- MySQL Connector J - Database connectivity
- Jedis - Redis client

**Frontend:**
- Axios 1.13.x - HTTP client for API calls
- Pinia 3.0.x - State management
- Vue Router 4.6.4 - Client-side routing
- Lucide Vue 0.469.x - Icon library

## Configuration

**Environment:**
- Spring profiles (dev/prod) - Environment-specific configuration
- YAML configuration files for Spring Boot
- TypeScript configuration for frontend
- Vite configuration for build process

**Build:**
- Maven pom.xml - Backend build configuration
- package.json - Frontend dependency and script configuration
- tsconfig.json - TypeScript compiler options
- postcss.config.js - PostCSS configuration

## Platform Requirements

**Development:**
- Java 21+ JDK
- Maven 3.8+
- Node.js 18+
- npm 9+
- MySQL 8.0+
- Redis 6.0+

**Production:**
- Java 21+ runtime
- MySQL 8.0+ database
- Redis 6.0+ for caching
- Docker support for containerization

---

*Stack analysis: 2026-04-01*