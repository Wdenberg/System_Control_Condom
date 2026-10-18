# ADR 0010 — API agregada para Dashboard

## Status

Accepted

## Contexto

O frontend React precisa apresentar ocupação, movimentações, câmeras, alertas e estado da cancela. Montar esses indicadores a partir de várias APIs de domínio aumentaria acoplamento e chamadas.

## Decisão

Criar uma camada `DashboardService` e endpoints agregados sob `/api/v1/dashboard`. A camada somente consulta os domínios existentes; ela não executa regras de acesso nem aciona a cancela.

Endpoints: `summary`, `activity`, `occupancy`, `cameras` e `alerts`.

As respostas agregadas usam Redis como cache de curta duração, com TTL padrão de 5 segundos e fallback para PostgreSQL. O fuso operacional padrão é `America/Recife`.

## Consequências

### Positivas

- Frontend recebe contratos estáveis e orientados à tela.
- Reduz número de consultas repetidas em dashboard.
- Cache curto evita dados excessivamente antigos.
- Redis indisponível não derruba a funcionalidade de consulta.
- Regras de negócio continuam concentradas nos serviços de domínio.

### Negativas

- O dashboard pode apresentar até o TTL de atraso.
- Existe uma camada adicional de DTOs agregados.
- Eventos push ainda não estão incluídos.

## Fora de escopo

SSE/WebSocket e processamento de eventos em tempo real ficam para uma fase específica.
