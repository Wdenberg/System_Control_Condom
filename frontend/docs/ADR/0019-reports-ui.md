# ADR 0019 — Interface de relatórios

## Status
Accepted

## Decisão
Criar uma tela `/reports` com três visões: Acessos, Ocupação e Cancela.

A tela de acessos usa paginação do backend, resumo agregado e exportação CSV. Ocupação é apresentada como snapshot atual. O relatório de cancela possui período, status e paginação.

O frontend não replica regras de autorização; apenas respeita `REPORT_READ` na rota. O backend continua como autoridade.
