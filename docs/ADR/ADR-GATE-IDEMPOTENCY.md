# ADR — Claim de comando e resultado físico incerto

Status: implementada; concorrência com PostgreSQL e hardware ainda não homologadas.

Separar claim transacional, HTTP sem transação e conclusão transacional. Claim usa SKIP LOCKED, token exclusivo e lease. Tentativas guardam histórico. Lease expirada/timeouts viram UNKNOWN e nunca são repetidos automaticamente. Apenas falha comprovadamente anterior ao envio/rejeição explícita pode retornar à fila. A identidade de comando é estável para deduplicação por fabricante, quando suportada. Exactly-once físico não é prometido.

Automação física desabilitada por padrão, com aprovação de comissionamento exigida no startup. Testes de abertura da aplicação limitados ao driver simulado. Não introduzir broker distribuído: release suporta uma instância e concorrência no banco.
