CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGSERIAL PRIMARY KEY,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    title VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ai_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_conversations_created_by ON ai_conversations(created_by);
CREATE INDEX IF NOT EXISTS idx_ai_messages_conversation_id ON ai_messages(conversation_id);
