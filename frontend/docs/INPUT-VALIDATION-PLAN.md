# Plano de implementação — Validação de entradas e formatação de nomes

Arquivo de destino: docs/INPUT-VALIDATION-PLAN.md
Abrangência: frontend, com testes e critérios de aceite.

## 1. Objetivo e decisões

Padronizar as validações dos formulários, impedir requisições com dados inválidos e reforçar as regras na API. Acrescentar formatação automática dos nomes de
moradores e nomes completos de usuários.

Decisões acordadas:

- Formatar nomes ao sair do campo e novamente antes de salvar.
- Manter de, da, do, das, dos e e em minúsculas no meio do nome.
- Implementar validações no frontend .
- Reutilizar componentes e dicionários existentes, sem adicionar bibliotecas.
- Preservar endpoints, estrutura dos payloads, permissões e formato dos CSVs.
- Não alterar CSS, algoritmo de senha ou processamento automático de placas das câmeras.
- Não executar atualização de nomes em lote. A normalização será aplicada nos próximos cadastros e salvamentos individuais.

## 2. Regras de entrada e formatação

### Nomes próprios

Criar uma função pura formatProperName(value: string): string no frontend.

Comportamento:

- Remover espaços nas extremidades e reduzir espaços consecutivos a um.
- Normalizar Unicode para NFC, preservando acentos.
- Converter cada palavra para inicial maiúscula e restante minúsculo.
- Manter as partículas definidas em minúsculas, exceto quando iniciarem o nome.
- Capitalizar também segmentos separados por hífen ou apóstrofo, preservando o separador.
- Retornar string vazia para entrada vazia ou somente espaços.
- Ser idempotente: formatar duas vezes deve produzir o mesmo resultado.
- Não tentar inferir grafias especiais, acrescentar acentos ou exigir sobrenome.
  Entrada Resultado
  ━━━━━━━━━━━━━━━━━━━━━━━━━━ ━━━━━━━━━━━━━━━━━━━━━━━━━━
  wdenberg ramos de barros Wdenberg Ramos de Barros
  ────────────────────────── ──────────────────────────
  MARIA DA SILVA Maria da Silva
  ────────────────────────── ──────────────────────────
  josé dos santos e souza José dos Santos e Souza
  ────────────────────────── ──────────────────────────
  ana-maria d'ávila Ana-Maria D'Ávila
  ────────────────────────── ──────────────────────────
  de souza De Souza

Aplicar somente aos campos fullName de moradores e usuários. Não aplicar a login, senha, número de apartamento, placa ou texto livre.

No frontend, o onBlur atualiza o valor visível. No envio, calcular uma variável com o nome formatado e usá-la diretamente na validação e no payload, sem depender de
uma atualização assíncrona de estado.

No backend, normalizar antes de persistir nas operações de criação e alteração de moradores e usuários.

### Limites e obrigatoriedade

Campo Regra
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Número de apartamento/vaga Obrigatório; até 20 caracteres
─────────────────────────────────── ───────────────────────────────────────────────────────────
Nome de morador Obrigatório; até 160 caracteres
─────────────────────────────────── ───────────────────────────────────────────────────────────
Nome completo de usuário Entre 2 e 160 caracteres
─────────────────────────────────── ───────────────────────────────────────────────────────────
Login no cadastro de usuário Entre 3 e 80 caracteres após remover espaços externos
─────────────────────────────────── ───────────────────────────────────────────────────────────
Senha inicial/nova senha Entre 12 e 200 caracteres; rejeitar somente espaços
─────────────────────────────────── ───────────────────────────────────────────────────────────
Marca/modelo Opcionais; até 80 caracteres
─────────────────────────────────── ───────────────────────────────────────────────────────────
Cor Opcional; até 40 caracteres
─────────────────────────────────── ───────────────────────────────────────────────────────────
Placa cadastrada/manual Exatamente 7 caracteres alfanuméricos após normalização
Justificativa de movimentação Entre 5 e 1000 caracteres após remover espaços externos
─────────────────────────────────── ───────────────────────────────────────────────────────────
Justificativa de teste da cancela Entre 5 e 500 caracteres após remover espaços externos
─────────────────────────────────── ───────────────────────────────────────────────────────────
Moradores do veículo Pelo menos um; ativos e pertencentes ao mesmo apartamento

Senhas devem permanecer exatamente como digitadas: não aplicar trim, capitalização ou normalização ao valor enviado. Usar a verificação de espaços somente para
detectar ausência de conteúdo.

No login de uma conta existente, validar apenas preenchimento; não aplicar os limites de criação de contas.

## 3. Implementação

### Formulários e mensagens

- Centralizar validadores puros e limites em src/lib/validation, com mensagens nos dicionários de apresentação.
- Converter os modais de cadastro em formulários com onSubmit; definir explicitamente botões de envio e botões auxiliares.
- Usar noValidate e validação explícita para garantir mensagens em português. Manter atributos HTML coerentes como apoio à edição.
- Validar todos os campos antes da chamada à API e impedir novos envios enquanto a operação estiver em andamento.
- Mostrar erros junto aos campos, com aria-invalid, aria-describedby e foco no primeiro campo inválido.
- Estender Field para atender input, select e textarea com tipagem adequada.
- Após uma tentativa inválida, revalidar o campo quando for corrigido. Limpar erros ao abrir outro cadastro ou cancelar.
- Manter erros de rede, conflitos e regras dependentes do servidor em uma mensagem geral, sem atribuí-los indevidamente a um campo.
- Preservar integralmente ApiError.body e continuar usando retorno genérico para mensagens desconhecidas.

### Placa manual

- Compartilhar a regra de placa entre cadastro e movimentação manual no frontend: remover separadores, converter para maiúsculas e exigir 7 caracteres
  alfanuméricos.

- Validar a placa na entrada de createManual, antes de processar ou persistir qualquer movimentação.
- Reutilizar a mensagem conhecida de placa inválida e retornar HTTP 400.
- Não modificar o normalizador compartilhado com a integração Hikvision.
- Preservar as validações dos DTOs e alinhar os testes de requisições diretas à API.

### Vagas e vínculos

- Exigir apartamento para vagas do tipo ASSIGNED.
- Excluir vagas VISITOR, sem apartamento ou incompatíveis com o apartamento selecionado das opções de nova vinculação.
- Considerar atividade, disponibilidade e vínculos com outros veículos ativos.
- Na edição, incluir explicitamente a vaga atual, identificada como “vaga atual”, mesmo quando ocupada. Não torná-la disponível para outros veículos.
- Preservar o vínculo atual ao salvar alterações não relacionadas à vaga.
- Ao trocar o apartamento dos moradores, limpar seleções de moradores e vaga incompatíveis.
- Manter as verificações do backend como autoridade para concorrência e restrições de veículos dentro da garagem.

### Períodos de consulta

- Validar datas antes de serializar, consultar, paginar ou exportar.
- Nos relatórios, verificar início anterior ao fim e limite de 366 dias sobre o intervalo efetivo, incluindo os horários já utilizados pela tela e os valores padrão
  do backend.

- Manter consulta de um único dia válida.
- Na auditoria, quando ambas as datas estiverem preenchidas, exigir início anterior ao fim. Não introduzir limite de 366 dias nessa tela.
- Acrescentar a mesma validação de ordem no serviço de auditoria, atendendo listagem e CSV.
- Preservar filtros e datas digitadas quando houver erro; não disparar consulta ou exportação inválida.
- Impedir que respostas pendentes de filtros anteriores removam o erro atual ou exibam resultados como se pertencessem ao período inválido.

## 4. Testes e critérios de aceite

### Testes unitários e de componentes

- Formatação dos exemplos da tabela, acentos, apóstrofo tipográfico, hífen, espaços, entrada vazia e idempotência.
- Nome permanece inalterado durante a digitação, formata no blur e é enviado formatado mesmo sem perda prévia de foco.
- Valores abaixo, exatamente no limite e acima dos limites de cada campo.
- Campos obrigatórios vazios ou contendo somente espaços.
- Senhas válidas com espaços preservados integralmente.
- Placas com 6, 7 e 8 caracteres normalizados; minúsculas, separadores e entrada sem caracteres válidos.
- Nenhuma chamada à API quando o formulário estiver inválido.
- Envio por Enter, foco no primeiro erro e correção das mensagens.
- Vaga própria sem apartamento, vaga visitante, moradores incompatíveis e preservação da vaga atual ocupada.
- Períodos invertidos, datas iguais na auditoria, relatório de um dia, limite de 366 dias e extrapolação.
- Consulta e exportação utilizando os mesmos filtros válidos.

### Verificação final

- Frontend: executar npm test e npm run build.
- Conferir formulários por teclado e em larguras de 390 e 1280 px, usando dados simulados.
- Registrar resultados e eventuais limitações ambientais; verificações bloqueadas não devem ser descritas como aprovadas.

Aceite: entradas inválidas são bloqueadas com mensagens claras, nomes seguem a regra definida, a API valida chamadas diretas e os fluxos válidos continuam
funcionando sem mudanças na estrutura dos contratos.

## 5. Checklist de execução

- [ ] Criar validadores, limites e formatador de nomes.
- [ ] Aplicar validações e formatação aos formulários.
- [ ] Padronizar envio, foco e mensagens acessíveis.
- [ ] Corrigir opções e preservação dos vínculos de vaga.
- [ ] Validar períodos de consulta e exportação.
- [ ] Implementar testes de fronteira e regressão.
- [ ] Executar suítes, builds e conferência no navegador.
- [ ] Documentar resultados e mudanças de comportamento.
