# Garage Control — Fase 16 E2E

A Fase 16 valida o sistema de ponta a ponta com Playwright 1.63.0.

## Cobertura

- login real pela interface React;
- Dashboard protegido por JWT/RBAC;
- cadastro operacional real via API;
- evento ANPR no mesmo formato XML usado pela Hikvision;
- decisão de acesso autorizada;
- publicação SSE e atualização da Portaria;
- placa desconhecida e acesso negado;
- ATTENDANT sem acesso administrativo;
- token inválido do webhook;
- idempotência pelo UUID Hikvision.

O hardware físico da cancela não é acionado. `GATE_ENABLED=false` no ambiente E2E.

## Requisitos

- Docker funcionando;
- Node.js compatível com o frontend;
- npm.

## Execução recomendada

Da raiz:

```bash
./e2e/scripts/run-e2e.sh
```

O script cria um banco e Redis descartáveis, constrói o backend, espera o health check, instala Chromium e executa a suíte contra o frontend Vite real.

Ao terminar, os containers e volumes E2E são removidos.

## Execução manual

```bash
docker compose -f docker-compose.e2e.yml up -d --build
curl http://127.0.0.1:8080/actuator/health
cd e2e
npm install
npx playwright install chromium
npm test
```

Relatório:

```bash
npm run report
```

## Nota de segurança

O cenário E2E usa credenciais artificiais e banco descartável. Não use os dados de teste nas câmeras reais, nem habilite a cancela física no ambiente E2E.
