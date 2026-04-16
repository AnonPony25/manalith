import { create } from 'zustand'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type ToastVariant = 'success' | 'error' | 'info' | 'warning'

interface Toast {
  id: string
  variant: ToastVariant
  title: string
  message?: string
}

interface ToastState {
  toasts: Toast[]
  addToast: (variant: ToastVariant, title: string, message?: string) => void
  removeToast: (id: string) => void
}

// ---------------------------------------------------------------------------
// Store
// ---------------------------------------------------------------------------

let nextId = 0

const useToastStore = create<ToastState>((set) => ({
  toasts: [],

  addToast(variant, title, message) {
    const id = String(++nextId)
    set((s) => ({ toasts: [...s.toasts, { id, variant, title, message }] }))

    // Auto-dismiss after 4 seconds
    setTimeout(() => {
      set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }))
    }, 4000)
  },

  removeToast(id) {
    set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }))
  },
}))

// ---------------------------------------------------------------------------
// Public hook
// ---------------------------------------------------------------------------

interface UseToastReturn {
  success: (title: string, message?: string) => void
  error: (title: string, message?: string) => void
  info: (title: string, message?: string) => void
  warning: (title: string, message?: string) => void
}

function useToast(): UseToastReturn {
  const addToast = useToastStore((s) => s.addToast)

  return {
    success: (title, message?) => addToast('success', title, message),
    error: (title, message?) => addToast('error', title, message),
    info: (title, message?) => addToast('info', title, message),
    warning: (title, message?) => addToast('warning', title, message),
  }
}

export { useToast, useToastStore }
export type { Toast, ToastVariant, ToastState }
