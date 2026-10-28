-- Base de políticas (US01). Os privilégios vêm do ALTER DEFAULT PRIVILEGES da V1:
-- agente_leitura só SELECT; agente_ingestao SELECT, INSERT, UPDATE e DELETE.

CREATE TABLE conhecimento.documento (
    id                 uuid PRIMARY KEY,
    titulo             varchar(200) NOT NULL,
    versao             varchar(20)  NOT NULL,
    vigente_desde      date         NOT NULL,
    situacao           varchar(20)  NOT NULL
        CHECK (situacao IN ('PROCESSANDO', 'AUTORIZADO', 'NAO_AUTORIZADO', 'EM_REVISAO', 'ERRO_PROCESSAMENTO')),
    hash_conteudo      char(64)     NOT NULL UNIQUE,
    texto              text         NOT NULL,          -- guardado para permitir reindexar
    quantidade_trechos int          NOT NULL DEFAULT 0,
    modelo_embedding   varchar(60)  NOT NULL,
    arquivo_origem     varchar(200) NOT NULL,
    cadastrado_por     varchar(100) NOT NULL,          -- quem enviou (constituição VI)
    cadastrado_em      timestamptz  NOT NULL,
    CONSTRAINT uq_documento_titulo_versao UNIQUE (titulo, versao)
);

-- Formato exigido pelo PgVectorStore 2.0.2 (conferido no fonte: id uuid, content text,
-- metadata json, embedding vector(N)). initialize-schema=false: o esquema é desta migração.
CREATE TABLE conhecimento.vector_store (
    id        uuid PRIMARY KEY,
    content   text,
    metadata  json,
    embedding vector(1536)
);

CREATE INDEX vector_store_embedding_hnsw
    ON conhecimento.vector_store USING hnsw (embedding vector_cosine_ops);

-- Remoção e atualização de situação filtram por documentoId dentro do metadata.
CREATE INDEX vector_store_documento_id
    ON conhecimento.vector_store ((metadata ->> 'documentoId'));
