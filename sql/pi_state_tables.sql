CREATE DATABASE IF NOT EXISTS pi_state;
USE PI_STATE;

CREATE TABLE IF NOT EXISTS downlink (
    id BIGINT NOT NULL AUTO_INCREMENT,
    frame_id VARCHAR(64) NOT NULL,
    serial_number VARCHAR(64) NOT NULL,
    frame TEXT,
    requires_ack BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at TIMESTAMP(6) NULL,
    ack_at TIMESTAMP(6) NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_downlink_frame_id (frame_id),

    KEY idx_downlink_serial_created (serial_number, created_at),
    KEY idx_downlink_requires_ack (requires_ack),
    KEY idx_downlink_sent_ack (sent_at, ack_at)
);

CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dedupe_key VARCHAR(128),
    component_id VARCHAR(64),
    status_en VARCHAR(32) NOT NULL,
    status_rank INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    expire_at BIGINT NOT NULL,
    downlink_id BIGINT NOT NULL,

    PRIMARY KEY (id),

    -- Enforce 1:1 relationship with downlink
    UNIQUE KEY uk_jobs_downlink_id (downlink_id),

    -- Optional but useful indexes
    KEY idx_jobs_status_rank_created (status_en, status_rank, created_at),
    KEY idx_jobs_component_status (component_id, status_en),
    KEY idx_jobs_dedupe_key (dedupe_key),
    KEY idx_jobs_expire_at (expire_at),

    CONSTRAINT fk_jobs_downlink
        FOREIGN KEY (downlink_id)
        REFERENCES downlink(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE

);

CREATE TABLE IF NOT EXISTS downlink_delivery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    downlink_id BIGINT NOT NULL,
    status_en VARCHAR(32) NOT NULL,
    enqueued_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    attempts INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP(6) NULL,
    in_flight_since TIMESTAMP(6) NULL,
    expire_at TIMESTAMP(6) NULL,

    PRIMARY KEY (id),

    -- Enforce 1:1 relationship with downlink
    UNIQUE KEY uk_downlink_delivery_downlink_id (downlink_id),

    -- Common scheduler/dispatcher access patterns
    KEY idx_downlink_delivery_status_enqueued (status_en, enqueued_at),
    KEY idx_downlink_delivery_inflight (status_en, in_flight_since),
    KEY idx_downlink_delivery_expire (expire_at),

    CONSTRAINT fk_downlink_delivery_downlink
        FOREIGN KEY (downlink_id)
        REFERENCES downlink(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS metric_cache (
    id BIGINT NOT NULL AUTO_INCREMENT,
    component_id VARCHAR(64) NOT NULL,
    metric VARCHAR(64) NOT NULL,
    metric_value DOUBLE NOT NULL,
    updated_at_ms BIGINT NOT NULL,
    PRIMARY KEY (id),
	
    UNIQUE KEY uk_metric_cache_component_metric (component_id, metric),
    KEY idx_metric_cache_component (component_id),
    KEY idx_metric_cache_updated_at (updated_at_ms),
    KEY idx_metric_cache_comp_metric_updated (component_id, metric, updated_at_ms)
);

select * from downlink;

select * from jobs;
