type ToastType = 'success' | 'error' | 'info' | 'warning'

interface ToastOptions {
  message: string
  type?: ToastType
  duration?: number
}

const getIcon = (type: ToastType): string => {
  const icons = {
    success: '✓',
    error: '✕',
    info: 'ℹ',
    warning: '⚠'
  }
  return icons[type]
}

const getColors = (type: ToastType): string => {
  const colors = {
    success: 'bg-emerald-500 dark:bg-emerald-600',
    error: 'bg-red-500 dark:bg-red-600',
    info: 'bg-blue-500 dark:bg-blue-600',
    warning: 'bg-amber-500 dark:bg-amber-600'
  }
  return colors[type]
}

export const toast = (options: ToastOptions | string) => {
  const opts = typeof options === 'string' ? { message: options } : options
  const { message, type = 'info', duration = 3000 } = opts

  const container = document.createElement('div')
  container.className = `
    fixed top-4 right-4 z-[9999] flex items-center gap-2 px-4 py-3 rounded-xl
    ${getColors(type)} text-white shadow-lg
    transform transition-all duration-300 translate-x-full
  `

  const icon = document.createElement('span')
  icon.textContent = getIcon(type)
  icon.className = 'text-lg'

  const text = document.createElement('span')
  text.textContent = message

  container.appendChild(icon)
  container.appendChild(text)
  document.body.appendChild(container)

  requestAnimationFrame(() => {
    container.classList.remove('translate-x-full')
  })

  setTimeout(() => {
    container.classList.add('translate-x-full', 'opacity-0')
    setTimeout(() => container.remove(), 300)
  }, duration)
}

export const success = (message: string) => toast({ message, type: 'success' })
export const error = (message: string) => toast({ message, type: 'error' })
export const info = (message: string) => toast({ message, type: 'info' })
export const warning = (message: string) => toast({ message, type: 'warning' })

export default toast
