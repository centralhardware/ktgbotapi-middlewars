CREATE TABLE IF NOT EXISTS bot_requests (
    timestamp   DateTime64(3),
    bot         LowCardinality(String),
    update_id   Int64 DEFAULT 0,
    user_id     Int64 DEFAULT 0,
    username    LowCardinality(String) DEFAULT '',
    first_name  LowCardinality(String) DEFAULT '',
    last_name   LowCardinality(String) DEFAULT '',
    method      String,
    request     String,
    response    String,
    success     Bool,
    error       String,
    duration_ms UInt32
) ENGINE = MergeTree
PARTITION BY toDate(timestamp)
ORDER BY (timestamp, bot, update_id);
