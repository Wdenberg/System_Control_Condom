import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import GateStatusBadge from '@/components/ui/GateStatusBadge';

describe('GateStatusBadge', () => {
  it('renders SENT status', () => {
    render(<GateStatusBadge status="SENT" />);
    expect(screen.getByText('Enviado')).toBeInTheDocument();
  });

  it('renders PENDING status', () => {
    render(<GateStatusBadge status="PENDING" />);
    expect(screen.getByText('Pendente')).toBeInTheDocument();
  });

  it('renders EXHAUSTED status', () => {
    render(<GateStatusBadge status="EXHAUSTED" />);
    expect(screen.getByText('Esgotado')).toBeInTheDocument();
  });

  it('renders DISABLED status', () => {
    render(<GateStatusBadge status="DISABLED" />);
    expect(screen.getByText('Desabilitado')).toBeInTheDocument();
  });

  it('has aria-label', () => {
    render(<GateStatusBadge status="SENT" />);
    expect(screen.getByText('Enviado')).toHaveAttribute('aria-label', 'Situação: Enviado');
  });
});
