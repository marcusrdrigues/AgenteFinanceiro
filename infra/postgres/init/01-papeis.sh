#!/bin/sh
# Cria a extensão vector e os papéis do banco financecorp. Roda uma vez, como superusuário, na
# primeira subida do container. É .sh (e não .sql) porque as senhas vêm de variáveis de ambiente.
#
# Papéis (constituição VII; data-model §1.3):
#   agente_migracao  dono dos schemas; usado só pelo Flyway
#   agente_ingestao  escreve só em "conhecimento"; usado só pelo perfil "ingestao"
#   agente_leitura   só SELECT; datasource da aplicação
# Os privilégios por schema são concedidos na migração V1__esquemas.sql.
set -eu

: "${POSTGRES_MIGRACAO_PASSWORD:?defina POSTGRES_MIGRACAO_PASSWORD}"
: "${POSTGRES_INGESTAO_PASSWORD:?defina POSTGRES_INGESTAO_PASSWORD}"
: "${POSTGRES_APP_PASSWORD:?defina POSTGRES_APP_PASSWORD}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -v senha_migracao="$POSTGRES_MIGRACAO_PASSWORD" \
    -v senha_ingestao="$POSTGRES_INGESTAO_PASSWORD" \
    -v senha_leitura="$POSTGRES_APP_PASSWORD" <<'SQL'
-- Extensão exige superusuário; por isso fica aqui e não numa migração.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE ROLE agente_migracao LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'senha_migracao';
CREATE ROLE agente_ingestao LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'senha_ingestao';
CREATE ROLE agente_leitura  LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'senha_leitura';

-- Só o papel de migração cria schemas no banco.
SELECT format('GRANT CREATE ON DATABASE %I TO agente_migracao', current_database()) \gexec
SQL
