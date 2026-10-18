import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import FilterBar from '@/components/ui/FilterBar';

describe('FilterBar', () => {
  it('renders children and count', () => {
    render(<FilterBar count={5} countLabel="vagas"><span>Filtro</span></FilterBar>);
    expect(screen.getByText('Filtro')).toBeInTheDocument();
    expect(screen.getByText('5 vagas')).toBeInTheDocument();
  });

  it('renders with default countLabel', () => {
    render(<FilterBar count={3}><span>Filtro</span></FilterBar>);
    expect(screen.getByText('3 itens')).toBeInTheDocument();
  });

  it('hides count when not provided', () => {
    render(<FilterBar><span>Filtro</span></FilterBar>);
    expect(screen.queryByText(/itens/)).not.toBeInTheDocument();
  });
});
