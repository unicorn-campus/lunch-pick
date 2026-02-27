'use client'

import { ButtonHTMLAttributes, ReactNode } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'text'
  size?: 'sm' | 'md' | 'lg' | 'full'
  loading?: boolean
  children: ReactNode
}

export default function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  children,
  className = '',
  ...props
}: ButtonProps) {
  const baseClass =
    'inline-flex items-center justify-center font-medium rounded-[var(--radius-m)] transition-all duration-[var(--duration-fast)] cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed focus-visible:outline-2 focus-visible:outline-[var(--color-primary)]'

  const variantClass = {
    primary: 'bg-[var(--color-primary)] text-white hover:bg-[var(--color-primary-hover)] active:scale-[0.97]',
    secondary: 'bg-transparent text-[var(--color-text-primary)] border border-[var(--color-border)] hover:bg-[var(--color-background)] active:scale-[0.97]',
    danger: 'bg-[var(--color-error)] text-white hover:opacity-90 active:scale-[0.97]',
    text: 'bg-transparent text-[var(--color-primary)] hover:opacity-70',
  }[variant]

  const sizeClass = {
    sm: 'px-3 py-1.5 text-[var(--font-size-caption)]',
    md: 'px-4 py-2.5 text-[var(--font-size-label)]',
    lg: 'px-6 py-3 text-[var(--font-size-body1)]',
    full: 'w-full px-4 py-3 text-[var(--font-size-label)]',
  }[size]

  return (
    <button
      className={`${baseClass} ${variantClass} ${sizeClass} ${className}`}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? (
        <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
      ) : (
        children
      )}
    </button>
  )
}
