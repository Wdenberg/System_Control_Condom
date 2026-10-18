import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ErrorBoundary from '@/components/ErrorBoundary';

function BrokenWrapper() {
  return <Broken />;
}

function Broken(): React.ReactElement {
  throw new Error('Test error');
}

describe('ErrorBoundary', () => {
  it('renders children when no error', () => {
    render(
      <ErrorBoundary>
        <div>Working content</div>
      </ErrorBoundary>
    );
    expect(screen.getByText('Working content')).toBeInTheDocument();
  });

  it('renders error UI when child throws', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <ErrorBoundary>
        <BrokenWrapper />
      </ErrorBoundary>
    );
    expect(screen.getByText('Algo deu errado')).toBeInTheDocument();
    expect(screen.getByText('Recarregar página')).toBeInTheDocument();
  });
});
