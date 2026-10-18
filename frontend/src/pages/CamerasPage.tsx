import { useEffect, useState } from 'react';
import { api } from '@/services/api';
import type { Camera } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import LoadingState from '@/components/LoadingState';
import EmptyState from '@/components/EmptyState';
import { Camera as CameraIcon } from 'lucide-react';
import { messages } from '@/lib/presentation/messages';
import { labels } from '@/lib/presentation/labels';

export default function CamerasPage() {
  const [items, setItems] = useState<Camera[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => { api.cameras.list().then(setItems).catch((err: unknown) => setError(err instanceof Error ? err.message : messages.errors.loadCameras)).finally(() => setLoading(false)); }, []);
  return <div className="page-content"><PageHeader title={labels.cameras.title} description={labels.cameras.description} />{error && <div className="banner warning" role="alert">{error}</div>}<section className="camera-card-grid">{loading ? <LoadingState /> : items.length === 0 ? <EmptyState title={labels.cameras.emptyTitle} description={labels.cameras.emptyDesc} /> : items.map((camera) => <article className="panel camera-detail" key={camera.id}><div className="camera-detail-head"><div className={`camera-icon ${camera.enabled ? 'online' : 'offline'}`}><CameraIcon className="h-4 w-4" /></div><div><span className="eyebrow">{camera.direction === 'ENTRY' ? labels.cameras.entry : labels.cameras.exit}</span><h3>{camera.name}</h3></div><span className={camera.enabled ? 'online-label' : 'offline-label'}>{camera.enabled ? labels.common.connected : labels.common.disconnected}</span></div><dl><div><dt>{labels.cameras.ip}</dt><dd>{camera.ipAddress}</dd></div><div><dt>{labels.cameras.model}</dt><dd>{camera.model}</dd></div><div><dt>{labels.cameras.channel}</dt><dd>{camera.channelId}</dd></div><div><dt>{labels.cameras.manufacturer}</dt><dd>{camera.vendor}</dd></div></dl></article>)}</section></div>;
}
