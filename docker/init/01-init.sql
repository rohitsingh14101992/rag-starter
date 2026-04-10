-- Enable the pgvector extension for vector similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- ── Users ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Keep updated_at current automatically
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Conversations ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TRIGGER conversations_updated_at
    BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_created_at ON conversations(created_at DESC);

-- ── Messages ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);

-- ── RAG & Synchronization ───────────────────────────────────────────────────

-- Embeddings table for Vector Search
CREATE TABLE IF NOT EXISTS embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    block_type VARCHAR(50) NOT NULL,
    text_content TEXT,
    base64_data TEXT,
    mime_type VARCHAR(100),
    raw_html TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    embedding vector(384)
);

-- Add vector index for performance with cosine distance (HNSW)
CREATE INDEX IF NOT EXISTS embeddings_embedding_idx ON embeddings USING hnsw (embedding vector_cosine_ops);

-- File tracker table for Synchronization
CREATE TABLE IF NOT EXISTS file_tracker (
    file_path TEXT PRIMARY KEY,
    hash TEXT NOT NULL,
    last_modified TIMESTAMP NOT NULL
);

-- Keyword indices table for BM25 / Full-text search
CREATE TABLE IF NOT EXISTS keyword_indices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id TEXT NOT NULL,
    text_content TEXT NOT NULL,
    tsv tsvector
);

-- Add GIN index for fast full-text search
CREATE INDEX IF NOT EXISTS keyword_indices_tsv_idx ON keyword_indices USING gin(tsv);

-- ── Seed data (dev / testing only) ────────────────────────────────────────────
-- Password: "password"  (bcrypt cost=10 — change before going to production)
INSERT INTO users (email, password_hash)
VALUES ('abc@gmail.com', '$2b$10$KUio6k1SLYGGbnGd93X4WumPCocQ/nvGnqMAep8WvCOW/SUAenf2C')
ON CONFLICT (email) DO NOTHING;