import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { error as toastError } from './toast'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')?.trim()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
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
    console.error('Response error:', error)

    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 401:
          toastError('未授权，请重新登录')
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          toastError('拒绝访问')
          break
        case 404:
          toastError('请求的资源不存在')
          break
        case 500:
          toastError('服务器错误')
          break
        default:
          toastError(data?.msg || '请求失败')
      }
    } else if (error.request) {
      toastError('网络错误，请检查网络连接')
    } else {
      toastError(error.message || '请求失败')
    }

    return Promise.reject(error)
  }
)

export default {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config)
  },

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  },

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  },

  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config)
  }
}
