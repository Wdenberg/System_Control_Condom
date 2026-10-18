import { Sun, Moon, Monitor } from 'lucide-react';
import { labels } from '@/lib/presentation/labels';

type Theme = 'light' | 'dark' | 'system';

const options: { value: Theme; label: string; Icon: typeof Sun }[] = [
  { value: 'light', label: labels.theme.light, Icon: Sun },
  { value: 'dark', label: labels.theme.dark, Icon: Moon },
  { value: 'system', label: labels.theme.system, Icon: Monitor },
];

export default function ThemeToggle({ theme, onChange }: { theme: Theme; onChange: (t: Theme) => void }) {
  return (
    <div className="flex items-center gap-1" role="radiogroup" aria-label={labels.theme.ariaLabel}>
      {options.map(({ value, label, Icon }) => (
        <button
          key={value}
          type="button"
          role="radio"
          aria-checked={theme === value}
          aria-label={label}
          title={label}
          onClick={() => onChange(value)}
          className={`
            inline-flex items-center justify-center w-8 h-8 rounded-lg transition-colors
            ${theme === value
              ? 'bg-[var(--color-primary)] text-[var(--color-primary-foreground)]'
              : 'text-[var(--color-muted-foreground)] hover:bg-[var(--color-muted)] hover:text-[var(--color-foreground)]'
            }
          `}
        >
          <Icon className="h-4 w-4" />
        </button>
      ))}
    </div>
  );
}
