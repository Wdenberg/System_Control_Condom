# ADR-0009 — Controle da cancela por GateDriver + operações persistidas

## Status

Accepted

## Contexto

Na Fase 5 o sistema decide se uma movimentação de entrada/saída é autorizada, mas ainda não controla o equipamento físico.

O fabricante e protocolo da cancela/relé não foram definidos. O backend também deve continuar independente das câmeras Hikvision.

## Decisão

Criar uma porta de integração:

```java
GateDriver.open(GateOpenCommand)
```

com implementações substituíveis:

- `SIMULATED` para desenvolvimento local;
- `HTTP` como adaptador genérico para equipamentos que exponham API HTTP;
- futuras implementações específicas de fabricante/relé quando o protocolo real for confirmado.

O domínio não conhecerá HTTP, GPIO, relé ou APIs proprietárias.

Antes de acionar o equipamento, o backend persiste uma `gate_operation` vinculada unicamente ao `movement`.

O dispatcher busca operações `PENDING`, envia o comando e atualiza o estado.

Estados:

```text
PENDING → SENT
PENDING → PENDING → ... → EXHAUSTED

GATE_DISABLED → DISABLED
```

O identificador `operationKey` é enviado no header `X-Gate-Operation-Id` pelo driver HTTP. O equipamento final deve tratar esse identificador como chave idempotente quando suportado.

## Motivações

### Por que não chamar a cancela diretamente do MovementService?

Porque a chamada física é um efeito externo e não participa da transação PostgreSQL. Persistir a intenção primeiro permite retry, observabilidade e auditoria.

### Por que usar um driver simulado?

Permite executar todo o fluxo em localhost sem risco de abrir uma cancela real.

### Por que não implementar ainda um endpoint Hikvision para abrir a cancela?

A câmera possui recursos de I/O e a documentação da linha confirma capacidade de entrada/saída, porém o protocolo exato de acionamento e a ligação elétrica do portão precisam ser confirmados no equipamento instalado. A integração física não deve ser inventada.

## Consequências

### Positivas

- baixo acoplamento;
- testes locais sem hardware;
- retry persistente;
- operação auditável;
- futuro adaptador de fabricante isolado.

### Negativas

- existe uma camada adicional de persistência e dispatcher;
- sem suporte idempotente no controlador físico, uma falha entre acionamento e confirmação pode provocar repetição de abertura em retry.

## Segurança

Os endpoints administrativos usam RBAC:

- `GATE_READ` para consulta;
- `GATE_TEST` para teste manual;
- `GATE_WRITE` para retry manual.

O teste manual exige justificativa e gera auditoria.

## Referências

A documentação técnica da Hikvision informa suporte a IO de entrada/saída e aponta `ISAPI/System/IO/outputs` como interface disponível em equipamentos que suportam a capacidade. A linha do modelo utilizado também aparece em integrações de parceiros com ISAPI e IO. citeturn189841search0turn157241search0
