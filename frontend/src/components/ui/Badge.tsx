import { type FC, type ReactNode } from 'react'

interface BadgeProps {
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'gold'
  size?: 'sm' | 'md'
  children: ReactNode
}

const variantClasses: Record<NonNullable<BadgeProps['variant']>, string> = {
  default: 'bg-zinc-700 text-zinc-200',
  success: 'bg-green-900 text-green-300',
  warning: 'bg-yellow-900 text-yellow-300',
  danger: 'bg-red-900 text-red-300',
  info: 'bg-blue-900 text-blue-300',
  gold: 'bg-yellow-700 text-black font-semibold',
}

const sizeClasses: Record<NonNullable<BadgeProps['size']>, string> = {
  sm: 'px-1.5 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-sm',
}

const Badge: FC<BadgeProps> = ({ variant = 'default', size = 'md', children }) => {
  return (
    <span
      className={[
        'inline-flex items-center rounded-md font-medium',
        variantClasses[variant],
        sizeClasses[size],
      ].join(' ')}
    >
      {children}
    </span>
  )
}

export default Badge
export type { BadgeProps }
