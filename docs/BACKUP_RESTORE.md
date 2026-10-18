# Backup e restauração

Pré-requisitos: Bash, Python 3.12+, Docker Compose; secrets no arquivo de ambiente fora do Git. Nenhum .env é executado como código shell. O banco usa variáveis do ambiente do próprio container.

Em janela de manutenção, interromper ingress de eventos e gravações por procedimento operacional. Manter backend disponível para leitura dos arquivos. Somente após confirmar quiescência executar:

```bash
BACKUP_QUIESCED=yes ENV_FILE=.env.prod RETENTION_DAYS=30 bash deploy/scripts/backup-postgres.sh
```

Backup grava diretório temporário privado, pg_dump custom, arquivos Hikvision e manifesto SHA-256. Publicação do bundle é feita somente após sucesso. Retenção remove apenas bundles próprios mais antigos; falha não produz bundle final. Arquivos têm permissões restritas. Transferência criptografada/offsite e monitoramento são responsabilidades operacionais ainda a comprovar.

Restaurar somente em projeto isolado criado pelo script:

```bash
RESTORE_CONFIRM=ISOLATED bash deploy/scripts/restore-postgres.sh /caminho/backup.tar.gz
```

O script verifica estrutura, caminhos e hashes antes de extrair; cria PostgreSQL e volume de imagens separados, sem portas públicas ou container_name de produção. Executa pg_restore com falha imediata, confere referências/hashes das imagens restauradas e apresenta contagens. Não escreve sobre produção nem apaga os volumes restaurados. Registrar projeto, manifesto, contagens, duração e resultado no relatório operacional. Credenciais temporárias não são impressas. Backups legados SQL não são apagados; sua recuperação requer procedimento isolado específico.

NÃO VALIDADO operacionalmente: Docker não estava acessível no ambiente desta execução. Não há evidência de pg_dump/pg_restore ou recuperação real. Não considerar backup operacional até o ensaio com dados representativos ser aprovado.
