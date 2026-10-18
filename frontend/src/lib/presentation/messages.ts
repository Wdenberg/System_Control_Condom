export const messages = {
  errors: {
    loadApartments: 'Erro ao carregar apartamentos.',
    loadCameras: 'Erro ao carregar câmeras.',
    loadMovements: 'Erro ao carregar movimentações.',
    loadRoles: 'Erro ao carregar perfis.',
    loadUsers: 'Erro ao carregar usuários.',
    loadParking: 'Erro ao carregar vagas.',
    loadVehicles: 'Erro ao carregar veículos.',
    loadAudit: 'Não foi possível carregar a auditoria.',
    loadGateOperation: 'Não foi possível carregar a operação da portaria.',
    loadDashboard: 'Não foi possível carregar o painel.',
    loadResidents: 'Não foi possível carregar os moradores.',
    loadGateReport: 'Não foi possível carregar o relatório da cancela.',
    loadAccessReport: 'Não foi possível carregar o relatório de acessos.',
    loadOccupancyReport: 'Não foi possível carregar o relatório de ocupação.',
    loadObservability: 'Não foi possível consultar a observabilidade.',
    login: 'Não foi possível entrar.',
    exportAudit: 'Não foi possível exportar a auditoria.',
    exportCsv: 'Não foi possível exportar o CSV.',
  } as Record<string, string>,

  validation: {
    selectResident: 'Selecione pelo menos um morador.',
  } as Record<string, string>,

  auth: {
    invalidCredentials: 'Usuário ou senha inválidos.',
  } as Record<string, string>,

  dashboard: {
    noData: 'Sem dados do painel.',
  } as Record<string, string>,

  gate: {
    disabled: 'A cancela está desativada.',
    testSent: (direction: string) => `Comando de teste enviado para ${direction === 'ENTRY' ? 'entrada' : 'saída'}.`,
  } as Record<string, string | ((...args: unknown[]) => string)>,

  movement: {
    registered: (id: number, direction: string) => `Movimentação ${id} registrada: ${direction}.`,
  } as Record<string, (...args: unknown[]) => string>,
} as const;
