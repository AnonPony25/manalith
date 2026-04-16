import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  leftIcon?: ReactNode
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, leftIcon, className = '', id, ...rest }, ref) => {
    const inputId = id ?? (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined)

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-sm text-zinc-400 mb-1"
          >
            {label}
          </label>
        )}
        <div className="relative">
          {leftIcon && (
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400">
              {leftIcon}
            </span>
          )}
          <input
            ref={ref}
            id={inputId}
            className={[
              'bg-zinc-800 border rounded-md text-zinc-100 px-3 py-2 w-full',
              'placeholder:text-zinc-500',
              'transition-colors duration-150',
              'focus:ring-2 focus:ring-yellow-500 focus:border-yellow-500 focus:outline-none',
              error ? 'border-red-500' : 'border-zinc-700',
              leftIcon ? 'pl-10' : '',
              className,
            ].join(' ')}
            {...rest}
          />
        </div>
        {error && (
          <p className="mt-1 text-sm text-red-400">{error}</p>
        )}
      </div>
    )
  },
)

Input.displayName = 'Input'

export default Input
export type { InputProps }
