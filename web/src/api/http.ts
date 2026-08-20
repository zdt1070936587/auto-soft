import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import { clearToken, getToken } from '@/utils/token'

export interface ApiResult<T> {
  code: number
  msg: string
  data: T
  traceId?: string
}

let onUnauthorized: (() => void) | null = null

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

function isLoginRequest(config?: InternalAxiosRequestConfig) {
  const url = config?.url || ''
  return url.includes('/auth/login')
}

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    const payload = response.data
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      message.error(payload.msg || '请求失败')
      return Promise.reject(new Error(payload.msg || '请求失败'))
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const payload = error.response?.data
    const msg = payload?.msg
    if (status === 401) {
      if (isLoginRequest(error.config)) {
        message.error(msg || '用户名或密码错误')
        return Promise.reject(error)
      }
      clearToken()
      onUnauthorized?.()
      message.error(msg || '登录已失效')
      return Promise.reject(error)
    }
    if (status === 403) {
      message.error(msg || '无权限')
      return Promise.reject(error)
    }
    if (error.response) {
      message.error(msg || '请求失败')
      return Promise.reject(error)
    }
    message.error('无法连接后端，请确认 8080 已启动')
    return Promise.reject(error)
  },
)

export default http
