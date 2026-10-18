import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import LoadingState from '@/components/LoadingState';

describe('LoadingState', () => {
  it('renders default label', () => {
    render(<LoadingState />);
    expect(screen.getByText('Carregando...')).toBeInTheDocument();
  });

  it('renders custom label', () => {
    render(<LoadingState label="Consultando..." />);
    expect(screen.getByText('Consultando...')).toBeInTheDocument();
  });

  it('has status role', () => {
    render(<LoadingState />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
