# Coding Conventions

**Analysis Date:** 2026-04-01

## Naming Patterns

**Files:**
- PascalCase for Vue components: `ProjectCreation.vue`, `Dashboard.vue`, `UserForm.vue`
- kebab-case for views: `project-detail`, `create-project`, `world-setting`
- camelCase for TypeScript files: `userApi.ts`, `formatUtils.ts`, `requestClient.ts`
- snake_case for configuration files: `postcss.config.js`, `vite.config.ts`

**Functions:**
- camelCase for utility functions: `formatNumber`, `useTheme`, `createProject`
- camelCase for API methods: `getUserInfo`, `login`, `register`
- camelCase for component methods: `handleSubmit`, `openDrawer`, `setActiveTab`

**Variables:**
- camelCase for local variables: `token`, `formData`, `userInfo`
- PascalCase for interfaces and types: `LoginForm`, `UserInfo`, `ProjectData`
- kebab-case for CSS class names and data attributes

**Types:**
- PascalCase for all interfaces and types
- TypeScript with explicit typing for function parameters and returns
- Generic types for API responses: `Promise<LoginResult>`, `AxiosResponse<UserInfo>`

## Code Style

**Formatting:**
- Prettier not detected - formatting appears to use editor defaults
- Tab indentation not enforced - spaces may be used inconsistently
- Line length not restricted - long lines present in codebase

**Linting:**
- ESLint not detected in project configuration
- No custom lint rules defined
- TypeScript compiler used via `tsc` in build process

**TypeScript:**
- Strict mode enabled in tsconfig
- Path aliases configured: `@/*` maps to `src/*`
- DOM types included, Node.js types available

**Vue 3:**
- Composition API with `<script setup>` syntax
- TypeScript for component props and events
- Single File Components (SFC) with `.vue` extension

## Import Organization

**Order:**
1. Built-in imports: `import { ref, reactive } from 'vue'`
2. Third-party imports: `import axios from 'axios'`, `import { defineStore } from 'pinia'`
3. Local imports with alias: `import request from '@/utils/request'`
4. Type imports: `import type { UserInfo } from '@/types'`

**Path Aliases:**
- `@/` maps to `src/` directory
- Used consistently across imports: `@/api/user`, `@/types/user`, `@/components/ui`

**Module Structure:**
- Barrel exports for APIs: `export * from './user'`
- Reusable utilities in `src/utils/`
- Composables in `src/composables/`
- Stores in `src/stores/`

## Error Handling

**Patterns:**
- Try-catch blocks for async operations
- Error propagation with Promise.reject()
- Toast notifications for user feedback: `toastError(res.msg)`
- Global error handling in Axios interceptors

**API Error Handling:**
```typescript
// Response interceptor handles common error codes
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== undefined) {
      if (res.code === 0) {
        return res.data
      } else {
        toastError(res.msg || '请求失败')
        return Promise.reject(new Error(res.msg || 'Error'))
      }
    }
    return res.data || res
  },
  (error) => {
    // Handle HTTP status codes
    switch (status) {
      case 401:
        toastError('未授权，请重新登录')
        break
      case 403:
        toastError('拒绝访问')
        break
      case 404:
        toastError('请求的资源不存在')
        break
    }
    return Promise.reject(error)
  }
)
```

**Component Error Handling:**
- Async operations in try-catch
- Loading states for operations
- Graceful fallbacks for API failures

## Logging

**Framework:** Console logging used

**Patterns:**
- Error logging with console.error
- Debug logging in stores and API modules
- User-facing messages via toast notifications
- No structured logging framework detected

**Example:**
```typescript
try {
  userInfo.value = await userApi.getUserInfo()
} catch (error) {
  console.error('Failed to fetch user info:', error)
}
```

## Comments

**When to Comment:**
- File headers with description and purpose
- JSDoc for utility functions with parameters and returns
- Implementation comments for complex logic
- TODO/FIXME comments for pending work

**JSDoc/TSDoc:**
- Consistently used in utility functions
- Document parameters and return types
- Example in `formatNumber.ts`:
```typescript
/**
 * 格式化数字显示
 * @param num 数字
 * @returns 格式化后的字符串
 */
export function formatNumber(num: number | string): string {
  // implementation
}
```

## Function Design

**Size:**
- Functions generally kept under 20 lines
- Components split into smaller, focused components
- Composables for shared logic

**Parameters:**
- Destructured props in components
- Named parameters for clarity
- Optional parameters with defaults: `type?: ToastType`

**Return Values:**
- Explicit typing for returns
- Consistent return patterns in APIs
- Composition API refs wrapped with readonly()

## Module Design

**Exports:**
- Named exports for utilities and types
- Barrel exports for index files
- Default exports only for main modules

**Barrel Files:**
- Used in API modules: `src/api/index.ts`
- Used in type modules: `src/types/index.ts`
- Clean import structure for consumers

**Props Interface:**
```typescript
interface Props {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
  block?: boolean
}
```

---

*Convention analysis: 2026-04-01*