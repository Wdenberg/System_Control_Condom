# ADR-0014 — CRUD operacional no frontend

## Status
Aceito

## Contexto
A Fase 8 entregou autenticação e dashboard, mas os dados operacionais ainda dependiam de chamadas diretas à API. A Fase 9 precisa permitir administração diária de apartamentos, moradores, vagas e veículos.

## Decisão
Construir telas CRUD sobre os endpoints existentes do backend, usando modais para criação/edição e atualização local otimista após resposta de sucesso. Exclusão física não é exposta nesta fase; recursos são desativados pelo campo `active`, preservando histórico.

A UI filtra funcionalidades por permission, mas o backend continua como autoridade de autorização.

## Consequências
- menor acoplamento entre telas e regras de negócio;
- manutenção de histórico e referências dos movimentos;
- feedback de conflitos HTTP 409, validações e erros de domínio;
- os contratos TypeScript devem acompanhar os DTOs do backend;
- futuras fases podem introduzir paginação e busca server-side sem alterar o domínio.
