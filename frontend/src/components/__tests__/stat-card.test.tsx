import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import StatCard from '@/components/StatCard';

describe('StatCard', () => {
  it('renders label and value', () => {
    render(<StatCard label="Vagas livres" value={15} />);
    expect(screen.getByText('Vagas livres')).toBeInTheDocument();
    expect(screen.getByText('15')).toBeInTheDocument();
  });

  it('renders hint when provided', () => {
    render(<StatCard label="Vagas livres" value={15} hint="5 ocupadas" />);
    expect(screen.getByText('5 ocupadas')).toBeInTheDocument();
  });

  it('does not render hint when omitted', () => {
    render(<StatCard label="Vagas livres" value={15} />);
    expect(screen.queryByText('ocupadas')).not.toBeInTheDocument();
  });

  it('applies tone class', () => {
    const { container } = render(<StatCard label="Test" value={1} tone="success" />);
    expect(container.firstElementChild).toHaveClass('success');
  });
});
