import type { ReactNode } from 'react';

type BadgeVariant = 'success' | 'danger' | 'warning' | 'info' | 'neutral' | 'entry' | 'exit';

const variantClasses: Record<BadgeVariant, string> = {
  success: 'bg-[var(--color-success-soft)] text-[var(--color-success)]',
  danger: 'bg-[var(--color-danger-soft)] text-[var(--color-danger)]',
  warning: 'bg-[var(--color-warning-soft)] text-[var(--color-warning)]',
  info: 'bg-[var(--color-accent-soft)] text-[var(--color-primary)]',
  neutral: 'bg-[var(--color-muted)] text-[var(--color-muted-foreground)]',
  entry: 'bg-[var(--color-entry-bg)] text-[var(--color-entry-fg)]',
  exit: 'bg-[var(--color-exit-bg)] text-[var(--color-exit-fg)]',
};

export default function Badge({
  variant = 'neutral',
  icon,
  children,
  className = '',
  ...rest
}: {
  variant?: BadgeVariant;
  icon?: ReactNode;
  children: ReactNode;
  className?: string;
} & React.HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={`inline-flex items-center min-h-[23px] px-2 rounded-full text-[9px] font-extrabold uppercase tracking-wide ${variantClasses[variant]} ${className}`}
      {...rest}
    >
      {icon && <span className="mr-1 inline-flex">{icon}</span>}
      {children}
    </span>
  );
}
