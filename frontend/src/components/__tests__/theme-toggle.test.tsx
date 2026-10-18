import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ThemeToggle from '@/components/ThemeToggle';

describe('ThemeToggle', () => {
  it('renders three theme buttons', () => {
    render(<ThemeToggle theme="light" onChange={vi.fn()} />);
    expect(screen.getByRole('radio', { name: 'Claro' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Escuro' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Sistema' })).toBeInTheDocument();
  });

  it('marks current theme as checked', () => {
    render(<ThemeToggle theme="dark" onChange={vi.fn()} />);
    expect(screen.getByRole('radio', { name: 'Escuro' })).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByRole('radio', { name: 'Claro' })).toHaveAttribute('aria-checked', 'false');
  });

  it('calls onChange when a theme is selected', () => {
    const onChange = vi.fn();
    render(<ThemeToggle theme="light" onChange={onChange} />);
    fireEvent.click(screen.getByRole('radio', { name: 'Escuro' }));
    expect(onChange).toHaveBeenCalledWith('dark');
  });
});
