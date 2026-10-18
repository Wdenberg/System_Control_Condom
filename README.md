# Controle de Garagem LPR

Monólito modular Java 21 / Spring Boot, frontend React/TypeScript, PostgreSQL e Redis. Recebe eventos Hikvision autenticados por câmera e distingue autorização de passagem confirmada.

Remediação em andamento: [plano](docs/REMEDIATION_PLAN.md), [testes](docs/TEST_REPORT.md), [prontidão](docs/RELEASE_READINESS.md). A entrega ainda não está homologada para produção.

Análise técnica separada por frontend/backend, com estrutura, RBAC, infraestrutura e testes: [ARCHITECTURE_REVIEW.md](docs/ARCHITECTURE_REVIEW.md).

Frontend: `cd frontend`, `npm ci`, `npm run dev`. Backend: configurar ambiente e executar `cd backend && ./gradlew bootRun`. Para instalação completa consultar [runbook](docs/DEPLOYMENT_RUNBOOK.md). Câmeras exigem secrets distintos; o token compartilhado legado não autentica. Não habilitar automação física antes do comissionamento autorizado.

[Contrato HTTP](docs/API-CONTRACT.md), [RBAC](docs/RBAC_MATRIX.md), [ciclo da cancela](docs/GATE_OPERATION_LIFECYCLE.md) e [backup/restore](docs/BACKUP_RESTORE.md).
