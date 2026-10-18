CREATE TABLE IF NOT EXISTS news_article (
    id VARCHAR(64) PRIMARY KEY,
    title TEXT NOT NULL,
    source VARCHAR(120) NOT NULL,
    url TEXT NOT NULL UNIQUE,
    summary TEXT,
    category VARCHAR(120),
    published_at TIMESTAMPTZ,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_news_article_published_at ON news_article (published_at DESC NULLS LAST);

CREATE TABLE IF NOT EXISTS collection_status (
    id INTEGER PRIMARY KEY,
    last_attempt_at TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    last_error TEXT,
    last_item_count INTEGER NOT NULL DEFAULT 0
);
INSERT INTO collection_status (id) VALUES (1) ON CONFLICT (id) DO NOTHING;
