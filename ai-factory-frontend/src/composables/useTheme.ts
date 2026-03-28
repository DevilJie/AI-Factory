import { ref, watch } from 'vue'

export type Theme = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'ai-factory-theme'

// 共享状态
const theme = ref<Theme>('system')

const getSystemTheme = (): 'light' | 'dark' => {
  if (typeof window !== 'undefined') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return 'dark'
}

const applyTheme = (value: Theme) => {
  const effectiveTheme = value === 'system' ? getSystemTheme() : value
  const html = document.documentElement

  if (effectiveTheme === 'dark') {
    html.classList.add('dark')
    html.style.colorScheme = 'dark'
  } else {
    html.classList.remove('dark')
    html.style.colorScheme = 'light'
  }
}

// 立即初始化主题（模块加载时执行）
const initTheme = () => {
  const stored = localStorage.getItem(STORAGE_KEY) as Theme | null
  if (stored && ['light', 'dark', 'system'].includes(stored)) {
    theme.value = stored
  }
  applyTheme(theme.value)
}

// 在模块加载时立即初始化
if (typeof window !== 'undefined') {
  // DOM ready 后立即应用主题
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initTheme)
  } else {
    initTheme()
  }

  // 监听系统主题变化
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.addEventListener('change', () => {
    if (theme.value === 'system') {
      applyTheme('system')
    }
  })
}

export const useTheme = () => {
  const setTheme = (value: Theme) => {
    theme.value = value
    localStorage.setItem(STORAGE_KEY, value)
    applyTheme(value)
  }

  const toggleTheme = () => {
    const current = theme.value === 'system' ? getSystemTheme() : theme.value
    setTheme(current === 'dark' ? 'light' : 'dark')
  }

  // 监听主题变化
  watch(theme, (value) => {
    applyTheme(value)
  })

  return {
    theme,
    setTheme,
    toggleTheme,
    initTheme
  }
}

export default useTheme
