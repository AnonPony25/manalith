import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react'
import Spinner from './Spinner'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  leftIcon?: ReactNode
}

const variantClasses: Record<NonNullable<ButtonProps['variant']>, string> = {
  primary: 'bg-yellow-600 hover:bg-yellow-500 text-black font-semibold',
  secondary: 'bg-zinc-700 hover:bg-zinc-600 text-zinc-100',
  danger: 'bg-red-700 hover:bg-red-600 text-white',
  ghost: 'bg-transparent hover:bg-zinc-800 text-zinc-300',
}

const sizeClasses: Record<NonNullable<ButtonProps['size']>, string> = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2',
  lg: 'px-6 py-3 text-lg',
}

const spinnerSize: Record<NonNullable<ButtonProps['size']>, 'sm' | 'md'> = {
  sm: 'sm',
  md: 'sm',
  lg: 'md',
}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = 'primary',
      size = 'md',
      loading = false,
      leftIcon,
      disabled,
      className = '',
      children,
      ...rest
    },
    ref,
  ) => {
    const isDisabled = disabled || loading

    return (
      <button
        ref={ref}
        disabled={isDisabled}
        className={[
          'inline-flex items-center justify-center gap-2 rounded-md',
          'transition-colors duration-150',
          'focus:ring-2 focus:ring-yellow-500 focus:outline-none',
          'disabled:opacity-50 disabled:cursor-not-allowed',
          variantClasses[variant],
          sizeClasses[size],
          className,
        ].join(' ')}
        {...rest}
      >
        {loading ? (
          <Spinner
            size={spinnerSize[size]}
            className={variant === 'primary' ? 'text-black' : ''}
          />
        ) : (
          leftIcon
        )}
        {children}
      </button>
    )
  },
)

Button.displayName = 'Button'

export default Button
export type { ButtonProps }
