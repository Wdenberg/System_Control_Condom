# Fase 13 — Relatórios

## Backend
- GET /api/v1/reports/access
- GET /api/v1/reports/access/summary
- GET /api/v1/reports/access.csv
- GET /api/v1/reports/occupancy
- GET /api/v1/reports/gates

Todos exigem REPORT_READ.

Intervalo de acesso/cancela: `[from, to)`; padrão é o dia operacional atual em `DASHBOARD_ZONE_ID` quando omitido.

Limites:
- paginação até 100 por página;
- CSV até 50.000 linhas;
- período máximo de 366 dias.
