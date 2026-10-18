#!/usr/bin/env python3
"""Generate a documentation snapshot from the sole executable permission matrix."""
from pathlib import Path
import re,sys
root=Path(__file__).resolve().parents[1]
source=(root/'backend/src/main/java/com/estacionamento/api/domain/role/RolePermissions.java').read_text()
permissions=re.findall(r'\b[A-Z][A-Z_]+\b',(root/'backend/src/main/java/com/estacionamento/api/domain/role/Permission.java').read_text().split('public enum Permission {',1)[1])
roles={}
for role,body in re.findall(r'map\.put\(RoleCode\.(\w+),(.+?)(?=\n\s*map\.put|\n\s*return)',source,re.S):
 roles[role]=set(re.findall(r'Permission\.(\w+)',body))
roles['SUPER_ADMIN']=set(permissions)
if set(roles) != {'SUPER_ADMIN','ADMIN','OPERATOR','ATTENDANT','VIEWER'}:raise SystemExit('Cannot parse every role from the executable matrix')
order=['SUPER_ADMIN','ADMIN','OPERATOR','ATTENDANT','VIEWER']
text='# Matriz RBAC\n\nGerada de `RolePermissions.java`; não editar como segunda fonte de regras.\n\n| Permissão | '+' | '.join(order)+' |\n|---|'+'---|'*len(order)+'\n'
for permission in permissions:text+='| '+permission+' | '+' | '.join('Sim' if permission in roles.get(role,set()) else '—' for role in order)+' |\n'
text+='\nDecisão de menor privilégio: VIEWER não lê contas administrativas; OPERATOR não testa cancela. Leitura do estado da cancela preservada para a operação existente. SUPER_ADMIN continua protegido por verificações adicionais de alvo, conta própria e último administrador ativo.\n'
target=root/'docs/RBAC_MATRIX.md'
if '--check' in sys.argv:
 if not target.exists() or target.read_text()!=text:raise SystemExit('RBAC documentation differs from executable policy')
 print('RBAC documentation matches executable policy')
else:target.write_text(text)
