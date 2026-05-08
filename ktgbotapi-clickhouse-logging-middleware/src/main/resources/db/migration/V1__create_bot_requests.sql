CREATE TABLE IF NOT EXISTS bot_requests (
    timestamp   DateTime64(3),
    bot         LowCardinality(String),
    request_id  String,
    method      String,
    request     String,
    response    String,
    success     Bool,
    error       String,
    duration_ms UInt32
) ENGINE = MergeTree
PARTITION BY toDate(timestamp)
ORDER BY (timestamp, bot, request_id);
