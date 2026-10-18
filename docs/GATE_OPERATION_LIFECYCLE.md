# Ciclo de cancela e passagem

## Comando

PENDING -> PROCESSING -> SENT (aceitação do comando), EXHAUSTED (falhas definitivamente anteriores/rejeitadas) ou UNKNOWN (resultado incerto).

GateDispatchTransactions reivindica uma linha via FOR UPDATE SKIP LOCKED, grava claim_token, lease_until e histórico de tentativa. O commit acontece antes da chamada ao driver. GateDispatchWorker chama HTTP sem transação de banco aberta e conclui por outro método transacional, conferindo claim e estado. Lease expirada vira UNKNOWN; não retorna automaticamente a PENDING. O mesmo operation_key acompanha cada tentativa conhecida.

Falha de conexão não estabelecida e rejeições HTTP explícitas podem voltar a PENDING até maxAttempts. Timeout/interrupção, exceção após claim, HTTP 5xx e resposta sem resultado são UNKNOWN. Somente EXHAUSTED permite retry administrativo. UNKNOWN exige reconciliação; o protocolo HTTP genérico não garante exatamente uma abertura física. O fabricante deve comprovar deduplicação de X-Gate-Operation-Id/operationId antes de habilitar automação real.

## Passagem

AUTHORIZED -> GATE_REQUESTED -> COMMAND_ACCEPTED -> COMPLETED somente após observação operacional explícita. DENIED, FAILED, UNKNOWN e RECONCILIATION_REQUIRED representam exceções. Reconhecimento, autorização e HTTP 2xx nunca alteram sozinhos vehicles.inside_garage ou parking_spaces.status.

Uma reserva persistida impede outra autorização para o mesmo veículo/vaga enquanto a passagem está pendente. Não expira automaticamente: uma incerteza física não pode ser apagada pelo relógio. O endpoint POST /api/v1/movements/{id}/passage-confirmation exige MOVEMENT_OVERRIDE, passed e justificativa de 10 a 1000 caracteres. A tela Movimentações oferece o fluxo. Comando PENDING/PROCESSING impede reconciliação. Observação positiva aplica a ocupação com locks e encerra a reserva; negativa encerra sem fabricar ocupação. Operador deve verificar o controlador e eventuais comandos tardios antes de concluir uma operação UNKNOWN; nesse caso controllerSettled=true é obrigatório e auditado. Nenhum sensor ou confirmação automática foi inventado.

Dados históricos: migration V10 preserva ocupação anterior, marca tentativas autorizadas antigas RECONCILIATION_REQUIRED e mantém suas reservas desativadas para não inventar passagem nem violar unicidade histórica. É necessário inventário físico antes da entrada em operação. Veículos desconhecidos ou sem vaga precisam de correção cadastral/reconciliação específica antes de consolidar ocupação.

Override requer cadastro prévio do veículo, ausência de reserva pendente e ocupação compatível com a direção. O movimento recebe os vínculos do veículo/vaga sob lock antes de reservar e enfileirar o comando. Esse fluxo também cobre uma leitura negada por baixa confiança; não concede abertura a placas sem identidade persistida.

Eventos movement.created/updated, gate.updated e hikvision.received são publicados após commit. SSE é atualização de interface recuperável por consultas/polling; não é fila durável nem dispara comandos. Decidiu-se não introduzir outbox para esse consumidor visual; entrega durável e múltiplas instâncias continuam fora do suporte de release.

A migration coloca comandos PENDING do legado e seus movimentos autorizados em UNKNOWN para impedir abertura atrasada no rollout e exigir verificação do controlador na reconciliação. Confirmação, retry, conclusão e recuperação seguem bloqueio de movimento antes da operação; retry recusa passagem já finalizada. Claim só seleciona movimentos GATE_REQUESTED.

UNKNOWN bloqueia novas autorizações pela placa e alterações da vaga mesmo quando o histórico migrado não possui reserva ativa. Isso preserva o bloqueio físico sem inventar reservas únicas para históricos que podem conter múltiplas operações do mesmo veículo. A reconciliação explícita encerra esse impedimento.

Métricas de aceitação, falha, esgotamento e resultado incerto são atualizadas após commit por evento interno de tentativa concluída. Uma conclusão repetida ou fora do claim atual não gera nova contagem. Esses contadores não representam passagem física confirmada.
