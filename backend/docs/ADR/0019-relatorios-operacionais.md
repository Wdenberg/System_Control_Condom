# ADR 0019 — Relatórios operacionais

## Status
Accepted

## Contexto
A operação precisa consultar acessos, entradas, saídas, negados, overrides, ocupação atual e falhas/operações de cancela sem depender do dashboard.

## Decisão
Criar uma camada de `ReportService` e endpoints somente leitura protegidos por `REPORT_READ`.

Os relatórios de acesso usam intervalo `[from, to)`, filtros opcionais e paginação. A exportação CSV usa os mesmos filtros e possui limite operacional de 50.000 linhas.

O relatório de ocupação representa um **snapshot atual**, não um histórico. Histórico de ocupação exigiria uma série temporal própria e será tratado somente se necessário.

PostgreSQL permanece como fonte da verdade; Redis não é usado para garantir consistência dos relatórios.

## Consequências
- Consultas podem ser paginadas.
- CSV pode ser consumido pelo Excel/LibreOffice devido ao UTF-8 com BOM.
- O período máximo é 366 dias por consulta.
- O backend continua sendo a autoridade para autorização.
