import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import PlaceholderPage from '@/pages/PlaceholderPage';

describe('PlaceholderPage', () => {
  it('renders title and description', () => {
    render(<MemoryRouter><PlaceholderPage title="Câmeras" description="Gerenciamento de câmeras." /></MemoryRouter>);
    expect(screen.getByText('Câmeras')).toBeInTheDocument();
    expect(screen.getByText('Gerenciamento de câmeras.')).toBeInTheDocument();
  });

  it('shows evolution message', () => {
    render(<MemoryRouter><PlaceholderPage title="Teste" description="Desc" /></MemoryRouter>);
    expect(screen.getByText('Interface em evolução')).toBeInTheDocument();
  });
});
