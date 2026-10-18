import { Search } from 'lucide-react';
import { labels } from '@/lib/presentation/labels';

export default function SearchInput({
  value,
  onChange,
  placeholder = labels.searchInput.defaultPlaceholder,
  label = labels.searchInput.defaultLabel,
  className = '',
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  label?: string;
  className?: string;
}) {
  return (
    <div className={`search-wrapper ${className}`}>
      <Search className="search-icon" />
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        aria-label={label}
        className="search-input"
      />
    </div>
  );
}
