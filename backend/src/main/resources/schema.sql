CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(255) NOT NULL UNIQUE,
    priority INTEGER,
    current_stage VARCHAR(50),
    overall_state VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    notes VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS order_stage_status (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    stage VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    assignee VARCHAR(255),
    claimed_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    service_time_minutes BIGINT,
    notes VARCHAR(1024),
    exception_reason VARCHAR(1024),
    supervisor_notes VARCHAR(1024),
    approved_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE,
    checklist_state TEXT,
    CONSTRAINT uq_order_stage_status_order_stage UNIQUE (order_id, stage)
);

CREATE INDEX IF NOT EXISTS idx_order_stage_status_order_id ON order_stage_status(order_id);
CREATE INDEX IF NOT EXISTS idx_order_stage_status_stage ON order_stage_status(stage);

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
