import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import SearchInput from '@/components/ui/SearchInput';

describe('SearchInput', () => {
  it('renders with aria-label', () => {
    render(<SearchInput value="" onChange={vi.fn()} label="Buscar vaga" />);
    expect(screen.getByLabelText('Buscar vaga')).toBeInTheDocument();
  });

  it('renders with placeholder', () => {
    render(<SearchInput value="" onChange={vi.fn()} placeholder="Digite..." label="Buscar" />);
    expect(screen.getByPlaceholderText('Digite...')).toBeInTheDocument();
  });

  it('calls onChange when typing', () => {
    const onChange = vi.fn();
    render(<SearchInput value="" onChange={onChange} label="Buscar" />);
    fireEvent.change(screen.getByLabelText('Buscar'), { target: { value: 'abc' } });
    expect(onChange).toHaveBeenCalledWith('abc');
  });

  it('displays current value', () => {
    render(<SearchInput value="ABC1D23" onChange={vi.fn()} label="Buscar" />);
    expect(screen.getByDisplayValue('ABC1D23')).toBeInTheDocument();
  });
});
