-- Schemas e privilégios. Roda como agente_migracao (dono dos objetos criados aqui e nas próximas
-- migrações), por isso o ALTER DEFAULT PRIVILEGES vale para as tabelas que vierem depois.

CREATE SCHEMA IF NOT EXISTS financeiro AUTHORIZATION agente_migracao;
CREATE SCHEMA IF NOT EXISTS conhecimento AUTHORIZATION agente_migracao;

-- financeiro: dados financeiros fictícios. Só leitura para a aplicação; a ingestão nunca toca aqui
-- (constituição VII: nenhuma ferramenta escreve em dado financeiro).
GRANT USAGE ON SCHEMA financeiro TO agente_leitura;
ALTER DEFAULT PRIVILEGES FOR ROLE agente_migracao IN SCHEMA financeiro
    GRANT SELECT ON TABLES TO agente_leitura;

-- conhecimento: documentos de política e vetores. A aplicação só lê; a carga administrativa
-- (perfil "ingestao") precisa inserir, desautorizar versões e apagar documentos removidos (FR-045).
GRANT USAGE ON SCHEMA conhecimento TO agente_leitura, agente_ingestao;
ALTER DEFAULT PRIVILEGES FOR ROLE agente_migracao IN SCHEMA conhecimento
    GRANT SELECT ON TABLES TO agente_leitura;
ALTER DEFAULT PRIVILEGES FOR ROLE agente_migracao IN SCHEMA conhecimento
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO agente_ingestao;
