-- ============================================
-- Helpdesk AI - Database Initialization Script
-- ============================================

-- Ativa extensão pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Verifica instalação
SELECT * FROM pg_extension WHERE extname = 'vector';

-- ============================================
-- Tabela de usuários
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- ============================================
-- Tabela de documentos
-- ============================================
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    filename VARCHAR(255) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    total_chunks INT,
    status VARCHAR(50), -- 'processing', 'completed', 'failed'
    uploaded_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents(user_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);

-- ============================================
-- Tabela de chunks com vector embeddings
-- ============================================
CREATE TABLE IF NOT EXISTS chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT REFERENCES documents(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    embedding vector(1536), -- OpenAI text-embedding-3-small
    chunk_index INT,
    metadata JSONB, -- {page: 5, section: "API Reference"}
    created_at TIMESTAMP DEFAULT NOW()
);

-- Índice HNSW para busca vetorial rápida (melhor que IVFFlat para < 1M vetores)
CREATE INDEX IF NOT EXISTS chunks_embedding_idx ON chunks
USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON chunks(document_id);

-- ============================================
-- Tabela de conversas
-- ============================================
CREATE TABLE IF NOT EXISTS conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations(user_id);

-- ============================================
-- Tabela de mensagens
-- ============================================
CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20), -- 'user' ou 'assistant'
    content TEXT NOT NULL,
    citations JSONB, -- [{chunk_id, similarity_score, metadata}]
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at);

-- ============================================
-- Dados iniciais (opcional para desenvolvimento)
-- ============================================

-- Usuário de teste (senha: password123)
-- Hash bcrypt gerado para 'password123': $2a$10$5Z9Z3Z9Z3Z9Z3Z9Z3Z9Z3euH3P3P3P3P3P3P3P3P3P3P3P3P3P3P3
-- NOTA: Alterar a senha em produção!

-- INSERT INTO users (email, password_hash, name)
-- VALUES ('admin@helpdesk.ai', '$2a$10$5Z9Z3Z9Z3Z9Z3Z9Z3Z9Z3euH3P3P3P3P3P3P3P3P3P3P3P3P3P3P3', 'Admin User');

-- ============================================
-- Fim do script
-- ============================================
