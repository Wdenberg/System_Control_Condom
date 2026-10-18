import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import EmptyState from '@/components/EmptyState';

describe('EmptyState', () => {
  it('renders title and description', () => {
    render(<EmptyState title="Nenhuma vaga" description="Cadastre uma vaga." />);
    expect(screen.getByText('Nenhuma vaga')).toBeInTheDocument();
    expect(screen.getByText('Cadastre uma vaga.')).toBeInTheDocument();
  });

  it('has status role', () => {
    render(<EmptyState title="Teste" description="Desc" />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
