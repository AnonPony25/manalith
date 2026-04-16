import axios, {
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
} from 'axios'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface TokenPairResponse {
  accessToken: string
  refreshToken: string
  userId: string
  displayName: string
}

export interface UserProfile {
  id: string
  displayName: string
  email: string
  avatarUrl: string | null
}

// ---------------------------------------------------------------------------
// Axios instance
// ---------------------------------------------------------------------------

const axiosInstance: AxiosInstance = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// ---------------------------------------------------------------------------
// Request interceptor — attach Bearer token
// ---------------------------------------------------------------------------

axiosInstance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // Lazy import to avoid circular dependency at module init time
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { useAuthStore } = require('@/store/authStore')
  const accessToken: string | null = useAuthStore.getState().accessToken
  if (accessToken && config.headers) {
    config.headers['Authorization'] = `Bearer ${accessToken}`
  }
  return config
})

// ---------------------------------------------------------------------------
// Response interceptor — handle 401 with token refresh + retry
// ---------------------------------------------------------------------------

let isRefreshing = false
let pendingRequests: Array<() => void> = []

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest: AxiosRequestConfig & { _retry?: boolean } =
      error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      if (isRefreshing) {
        // Queue the request until the in-flight refresh completes
        return new Promise<void>((resolve) => {
          pendingRequests.push(resolve)
        }).then(() => axiosInstance(originalRequest))
      }

      isRefreshing = true

      try {
        // eslint-disable-next-line @typescript-eslint/no-var-requires
        const { useAuthStore } = require('@/store/authStore')
        const refreshed = await useAuthStore.getState().refreshToken()

        if (refreshed) {
          // Drain queued requests now that we have a new access token
          pendingRequests.forEach((resolve) => resolve())
          pendingRequests = []
          isRefreshing = false
          return axiosInstance(originalRequest)
        } else {
          // Refresh failed — log out
          pendingRequests = []
          isRefreshing = false
          await useAuthStore.getState().logout()
          return Promise.reject(error)
        }
      } catch {
        pendingRequests = []
        isRefreshing = false
        return Promise.reject(error)
      }
    }

    return Promise.reject(error)
  }
)

// ---------------------------------------------------------------------------
// Auth API surface
// ---------------------------------------------------------------------------

export const authApi = {
  /**
   * Exchange a raw refresh token for a new token pair.
   * Used by the Zustand store's {@code refreshToken()} action.
   */
  async refresh(refreshToken: string): Promise<TokenPairResponse> {
    const { data } = await axiosInstance.post<TokenPairResponse>('/auth/refresh', {
      refreshToken,
    })
    return data
  },

  /**
   * Revoke all refresh tokens for the authenticated user (server-side logout).
   * Requires a valid access token in the Authorization header.
   */
  async logout(): Promise<void> {
    await axiosInstance.post('/auth/logout')
  },

  /**
   * Fetch the current user's profile. Requires authentication.
   */
  async getMe(): Promise<UserProfile> {
    const { data } = await axiosInstance.get<UserProfile>('/auth/me')
    return data
  },
}

// Also export the underlying axios instance so other API modules can reuse it.
export { axiosInstance }
