CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('ADMIN', 'STAFF', 'USER')),
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    uploaded_by BIGINT REFERENCES users(id),
    uploaded_at TIMESTAMP DEFAULT now(),
    status VARCHAR(20) DEFAULT 'PROCESSED'
);

CREATE TABLE chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT REFERENCES documents(id),
    pinecone_vector_id VARCHAR(100) NOT NULL,
    chunk_text TEXT NOT NULL,
    page_number INT
);

CREATE TABLE conversation_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES conversation_sessions(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_messages_session ON messages(session_id);
CREATE INDEX idx_chunks_document ON chunks(document_id);
