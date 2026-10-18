# ADR-0018 — Gestão administrativa de usuários e RBAC

## Status
Aceito

## Contexto
A aplicação precisa permitir que administradores gerenciem contas sem duplicar a matriz de permissões em banco ou frontend.

## Decisão
Usuários são gerenciados por endpoints dedicados protegidos por `USER_READ` e `USER_WRITE`. Roles são um catálogo fixo representado por `RoleCode`; suas permissões continuam derivadas exclusivamente de `RolePermissions`. O endpoint de roles é somente leitura nesta fase.

A aplicação impede que o último `SUPER_ADMIN` ativo seja removido ou despromovido e impede que o próprio usuário autenticado desative ou altere sua role.

Não existe `DELETE` de usuário; a conta é desativada para preservar histórico e auditoria.

## Consequências
- RBAC continua centralizado.
- O frontend não consegue criar permissões arbitrárias.
- A administração de usuários pode evoluir sem alterar o domínio de autorização.
- Alterações de senha continuam sendo persistidas como hash.

Apenas `SUPER_ADMIN` pode criar, promover, desativar, alterar role ou redefinir senha de outra conta `SUPER_ADMIN`. `ADMIN` continua podendo administrar contas não privilegiadas.
