import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import PageHeader from '@/components/PageHeader';

describe('PageHeader', () => {
  it('renders title and description', () => {
    render(<PageHeader title="Vagas" description="Gerencie vagas." />);
    expect(screen.getByText('Vagas')).toBeInTheDocument();
    expect(screen.getByText('Gerencie vagas.')).toBeInTheDocument();
  });

  it('renders eyebrow when provided', () => {
    render(<PageHeader eyebrow="OPERAÇÃO" title="Vagas" description="Desc" />);
    expect(screen.getByText('OPERAÇÃO')).toBeInTheDocument();
  });

  it('does not render eyebrow when omitted', () => {
    render(<PageHeader title="Vagas" description="Desc" />);
    expect(screen.queryByText('OPERAÇÃO')).not.toBeInTheDocument();
  });

  it('renders action when provided', () => {
    render(<PageHeader title="Vagas" description="Desc" action={<button>Salvar</button>} />);
    expect(screen.getByText('Salvar')).toBeInTheDocument();
  });
});
