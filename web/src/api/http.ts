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

function rejectWithMessage(text: string, showToast = true) {
  if (showToast) {
    message.error({ content: text, key: 'http-error', duration: 3 })
  }
  return Promise.reject(new Error(text))
}

http.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    const payload = response.data
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      return rejectWithMessage(payload.msg || '请求失败')
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const payload = error.response?.data
    const msg = payload?.msg
    if (status === 401) {
      if (isLoginRequest(error.config)) {
        return rejectWithMessage(msg || '用户名或密码错误')
      }
      clearToken()
      onUnauthorized?.()
      return rejectWithMessage(msg || '登录已失效')
    }
    if (status === 403) {
      return rejectWithMessage(msg || '无权限')
    }
    if (error.response) {
      return rejectWithMessage(msg || '请求失败')
    }
    return rejectWithMessage('无法连接后端，请确认 8080 已启动')
  },
)

export default http
