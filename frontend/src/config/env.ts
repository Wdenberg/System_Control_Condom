export const env = {
  apiBaseUrl: (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, ''),
  prometheusUrl: import.meta.env.VITE_PROMETHEUS_URL || '',
  grafanaUrl: import.meta.env.VITE_GRAFANA_URL || '',
};
