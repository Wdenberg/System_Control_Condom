# ADR 0023 — Release 1.0.0 e homologação final

## Status
Accepted

## Contexto

O sistema possui backend, frontend, integração Hikvision, realtime, RBAC, relatórios, auditoria, observabilidade e infraestrutura Docker. A integração física da cancela ainda não possui controlador identificado.

## Decisão

Criar uma etapa final de homologação/release que valide software e ambiente sem bloquear a versão 1.0.0 pela ausência do hardware físico. O driver `SIMULATED` permanece padrão até que o controlador real seja conhecido.

## Consequências

- O software pode ser homologado e implantado sem acionar uma cancela real.
- A homologação física da barreira será um procedimento posterior específico do driver.
- Contratos Frontend ↔ Backend passam a ser tratados como parte do release.
