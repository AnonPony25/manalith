import { type FC, useEffect, useState } from 'react'
import { useToastStore, type Toast as ToastType, type ToastVariant } from './useToast'

// ---------------------------------------------------------------------------
// Icon helpers (inline SVG / unicode — no external icon library)
// ---------------------------------------------------------------------------

const iconConfig: Record<ToastVariant, { icon: string; bg: string; text: string }> = {
  success: { icon: '✓', bg: 'bg-green-700', text: 'text-green-100' },
  error: { icon: '✕', bg: 'bg-red-700', text: 'text-red-100' },
  info: { icon: 'ℹ', bg: 'bg-blue-700', text: 'text-blue-100' },
  warning: { icon: '⚠', bg: 'bg-yellow-700', text: 'text-yellow-100' },
}

const borderColors: Record<ToastVariant, string> = {
  success: 'border-green-700',
  error: 'border-red-700',
  info: 'border-blue-700',
  warning: 'border-yellow-700',
}

// ---------------------------------------------------------------------------
// Single toast
// ---------------------------------------------------------------------------

interface ToastItemProps {
  toast: ToastType
  onDismiss: (id: string) => void
}

const ToastItem: FC<ToastItemProps> = ({ toast, onDismiss }) => {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    // Trigger slide-in on next frame
    requestAnimationFrame(() => setVisible(true))
  }, [])

  const handleDismiss = () => {
    setVisible(false)
    // Wait for fade-out transition before removing from store
    setTimeout(() => onDismiss(toast.id), 200)
  }

  const { icon, bg, text } = iconConfig[toast.variant]
  const border = borderColors[toast.variant]

  return (
    <div
      role="alert"
      className={[
        'flex items-start gap-3 bg-zinc-900 border rounded-lg shadow-lg p-4 min-w-[300px] max-w-sm',
        'transition-all duration-200 ease-out',
        border,
        visible
          ? 'translate-x-0 opacity-100'
          : 'translate-x-full opacity-0',
      ].join(' ')}
    >
      {/* Icon circle */}
      <span
        className={[
          'flex-shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold',
          bg,
          text,
        ].join(' ')}
      >
        {icon}
      </span>

      {/* Content */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-zinc-100">{toast.title}</p>
        {toast.message && (
          <p className="mt-0.5 text-sm text-zinc-400">{toast.message}</p>
        )}
      </div>

      {/* Dismiss */}
      <button
        type="button"
        onClick={handleDismiss}
        className="flex-shrink-0 text-zinc-500 hover:text-zinc-200 transition-colors duration-150"
        aria-label="Dismiss"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Container — renders all toasts stacked top-right
// ---------------------------------------------------------------------------

const ToastContainer: FC = () => {
  const toasts = useToastStore((s) => s.toasts)
  const removeToast = useToastStore((s) => s.removeToast)

  if (toasts.length === 0) return null

  return (
    <div
      className="fixed top-4 right-4 z-50 flex flex-col gap-2 pointer-events-none"
      aria-live="polite"
    >
      {toasts.map((toast) => (
        <div key={toast.id} className="pointer-events-auto">
          <ToastItem toast={toast} onDismiss={removeToast} />
        </div>
      ))}
    </div>
  )
}

export { ToastContainer }
export default ToastContainer
