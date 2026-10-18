# ADR-0009 — Movimentações e regras de acesso

> Atualização: as decisões de autenticação, sessão e ocupação deste ADR foram substituídas pelos ADRs de remediação em `docs/ADR/ADR-WEBHOOK-AUTH.md`, `ADR-SESSION-REVOCATION.md` e `ADR-PASSAGE-CONFIRMATION.md` na raiz do projeto.


- **Status:** Accepted
- **Data:** 2026-09-13

## Contexto

A integração Hikvision já recebe eventos ANPR e o cadastro já relaciona placa, morador, apartamento e vaga. Faltava transformar essa leitura em uma decisão operacional de entrada/saída.

## Decisão

O domínio terá uma entidade `Movement` para cada tentativa de acesso relevante. Um movimento possui direção, origem, status e decisão.

### Direção

A direção operacional vem da câmera cadastrada:

- `192.168.15.43` → `ENTRY`
- `192.168.15.44` → `EXIT`

O campo `<direction>` do XML Hikvision é preservado como dado de captura, mas não define sozinho a entrada/saída da garagem.

### Entrada automática

Uma entrada só é `AUTHORIZED` quando:

1. a placa identifica um veículo ativo;
2. existe pelo menos um morador ativo em apartamento ativo;
3. o veículo possui vaga atribuída;
4. a vaga está ativa e não bloqueada;
5. a vaga está `AVAILABLE`;
6. o veículo não está marcado como `inside_garage`.

Ao autorizar, a transação marca o veículo como dentro e a vaga como ocupada.

### Saída automática

Uma saída só é `AUTHORIZED` quando o veículo está ativo e `inside_garage=true`. Ao autorizar, o veículo passa para fora e a vaga atribuída volta a `AVAILABLE` quando aplicável.

### Estado de ocupação

O MVP usa `vehicles.inside_garage` e `parking_spaces.status`. PostgreSQL é a fonte da verdade.

### Concorrência

A linha do veículo e, durante a transição, a linha da vaga são protegidas com lock pessimista. O objetivo é impedir que duas entradas concorrentes autorizem a mesma vaga.

### Movimento manual

O operador autenticado pode criar um movimento manual com `MOVEMENT_MANUAL_WRITE`. Toda operação manual exige justificativa e é auditada.

### Override

`MOVEMENT_OVERRIDE` pode transformar um movimento negado em `OVERRIDDEN`. A alteração do estado de ocupação só ocorre quando pode ser feita de forma consistente; o override não deve inventar que uma vaga ficou ocupada quando isso não pode ser provado pelo estado cadastral.

### Baixa confiança

Abaixo do limiar configurado em `app.parking.access.min-confidence`, eventos ANPR são registrados como `LOW_CONFIDENCE` e não passam para a autorização cadastral automática.

## Consequências

- As decisões ficam explícitas e testáveis.
- Eventos negados continuam no histórico.
- O sistema fica preparado para integração futura com a cancela.
- O modelo pode evoluir posteriormente para sensores físicos sem reescrever o histórico de movimentos.
