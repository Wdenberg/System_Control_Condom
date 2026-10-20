# Fase 19 — Remediação e homologação

Implementação em prioridades P0/P1/P2/P3 seguindo o documento de correção arquitetural. Esta fase NÃO representa aprovação de produção.

Em 20/10, frontend aprovado após npm ci: 276 testes em 42 arquivos, TypeScript, build e cobertura. Provider fixado no lockfile; linhas 59,21%, statements 53,17%, branches 51,65% e funções 43,39%.

Backend: ./gradlew clean test build --no-daemon --no-build-cache passou com 146/146 testes em 41 classes, incluindo RemediationIntegrationTest 25/25 e PostgreSQL/Redis reais; JAR gerado. Corrigida a desserialização do campo opcional controllerSettled, com regressão para ausência/null/false e bloqueio de UNKNOWN.

Playwright passou com 6/6 cenários Chromium. A primeira execução revelou erro 500 por leitura de moradores fora da transação, corrigido montando o DTO no serviço, com regressão integrada para listagem e detalhe. Os cenários agora aguardam login/SSE e navegam pela SPA. E2E usa projeto Compose exclusivo, npm ci e TypeScript 7.0.2. Upgrade com dados legados, CI hospedada, restauração e comissionamento físico ainda precisam de evidências próprias.

Ver docs/REMEDIATION_PLAN.md, docs/TEST_REPORT.md, docs/BACKEND_TEST_ANALYSIS.md e docs/RELEASE_READINESS.md para resultados e pendências.
