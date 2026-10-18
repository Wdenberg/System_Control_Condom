import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import MercosulPlate from '@/components/MercosulPlate';

describe('MercosulPlate', () => {
  it('renders plate text uppercase', () => {
    render(<MercosulPlate plate="abc1d23" />);
    expect(screen.getByText('ABC1D23')).toBeInTheDocument();
  });

  it('truncates plate to 7 characters', () => {
    render(<MercosulPlate plate="ABC1D23456" />);
    expect(screen.getByText('ABC1D23')).toBeInTheDocument();
  });

  it('shows placeholder when plate is empty', () => {
    render(<MercosulPlate plate="" />);
    expect(screen.getByText('-------')).toBeInTheDocument();
  });

  it('shows Mercosul header text', () => {
    render(<MercosulPlate plate="ABC1D23" />);
    expect(screen.getByText('Mercosul')).toBeInTheDocument();
    expect(screen.getByText('BRASIL')).toBeInTheDocument();
  });

  it('shows BR side label', () => {
    render(<MercosulPlate plate="ABC1D23" />);
    expect(screen.getByText('BR')).toBeInTheDocument();
  });

  it('has mercosul-plate wrapper class', () => {
    const { container } = render(<MercosulPlate plate="ABC1D23" />);
    expect(container.firstChild).toHaveClass('mercosul-plate');
  });
});
