import PageHeader from '@/components/PageHeader';
import { labels } from '@/lib/presentation/labels';
import { Construction } from 'lucide-react';

export default function PlaceholderPage({ title, description }: { title: string; description: string }) {
  return <div className="page-content"><PageHeader title={title} description={description} /><section className="panel placeholder-panel"><div className="placeholder-icon"><Construction className="h-10 w-10 text-[var(--color-muted-foreground)]" /></div><h3>{labels.placeholder.title}</h3><p>{labels.placeholder.description}</p></section></div>;
}
