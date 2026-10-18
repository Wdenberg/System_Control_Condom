# ADR — Confirmação de passagem e reservas

Status: implementada; validação operacional pendente. Substitui atualização de ocupação na autorização do ADR-0009.

O equipamento instalado não fornece confirmação de passagem comprovada no material disponível. Assim, reconhecimento, autorização e aceitação HTTP não consolidam ocupação. Adotou-se confirmação observada por operador com MOVEMENT_OVERRIDE e justificativa, endpoint protegido e auditoria. Reserva persistida bloqueia outra autorização até reconciliação. Não se inventa sensor nem se expira automaticamente uma incerteza física.

O histórico anterior é preservado e sinalizado para inventário/reconciliação; não é convertido ficticiamente em passagem confirmada. A migração é aditiva. Futuro adaptador de sensor deverá provar associação unívoca com movimento/operação antes de automatizar a confirmação. SSE continua projeção visual, com polling de recuperação; outbox durável não é necessário para acionamento porque este usa a fila persistida de comandos.

Override exige veículo previamente cadastrado e vincula o movimento ao veículo/vaga resolvidos sob lock, inclusive quando a leitura de baixa confiança não tinha esses vínculos. Pode superar a decisão cadastral com justificativa, mas não conflitos de ocupação ou reservas pendentes. Uma placa desconhecida deve ser cadastrada antes da autorização: sem essa identidade não seria possível reservar o veículo e impedir comandos concorrentes. A confirmação física continua obrigatória após o override.
