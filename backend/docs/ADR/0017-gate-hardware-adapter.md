# ADR 0017 — Adaptador de hardware da cancela

## Contexto

O controlador físico da cancela ainda não foi identificado. O sistema precisa ficar pronto para integração sem inventar um protocolo ou acoplar o domínio a um fabricante.

## Decisão

Manter `GateDriver` como porta de infraestrutura, com `SIMULATED` para desenvolvimento e `HTTP` como adaptador genérico. O status diferencia endpoint configurado de conectividade realmente verificada. O backend só considera `SENT` quando o driver retorna sucesso; isso não significa que a barreira física abriu se o hardware não fornecer feedback.

## Consequências

Quando o equipamento real for conhecido, será adicionado um novo driver/adaptador mantendo `GateOperationService`, regras de acesso e API estáveis.
