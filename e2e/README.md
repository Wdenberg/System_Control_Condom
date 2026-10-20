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

O script usa um projeto Compose exclusivo por execução, cria um banco e Redis descartáveis, constrói o backend, espera o health check, instala as dependências pelo lockfile e o Chromium e executa a suíte contra o frontend Vite real.

Ao terminar, os containers e volumes E2E são removidos.

Em falha, o script salva os logs Compose em `test-results/compose.log` antes de remover o ambiente. Playwright gera também `test-results/results.xml`, além de traces e relatório HTML.

Execução validada em 20/10/2026: seis cenários aprovados no Chromium; evidência em [../docs/validation/e2e-2026-10-20.json](../docs/validation/e2e-2026-10-20.json).

## Execução manual

```bash
docker compose -f docker-compose.e2e.yml up -d --build
curl http://127.0.0.1:8081/actuator/health/readiness
cd e2e
npm ci
npm run typecheck
npx playwright install chromium
npm test
```

Relatório:

```bash
npm run report
```

## Nota de segurança

O cenário E2E usa credenciais artificiais e banco descartável. Não use os dados de teste nas câmeras reais, nem habilite a cancela física no ambiente E2E.
