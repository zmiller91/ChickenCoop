CREATE DATABASE IF NOT EXISTS pi_state;
USE PI_STATE;

CREATE TABLE jobs (

    id BIGINT NOT NULL AUTO_INCREMENT,
    frame_id VARCHAR(64) NOT NULL,
    dedupe_key VARCHAR(128) NULL,
    component_id VARCHAR(64) NOT NULL,
    status_en VARCHAR(32) NOT NULL,
    command TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    status_rank INT NOT NULL,
    expire_at BIGINT NOT NULL,

    PRIMARY KEY (id),

    -- Fast lookup when ACK / COMPLETE events arrive
    UNIQUE KEY uk_jobs_frame_id (frame_id),

    -- Scheduler hydration & ordering
    KEY idx_jobs_status_created (status_en, created_at),

    -- Expiry & purge support
    KEY idx_jobs_expire_at (expire_at),

    -- Component-based queries / resource hydration
    KEY idx_jobs_component_status (component_id, status_en)
);

select * from jobs;