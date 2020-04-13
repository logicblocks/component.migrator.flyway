CREATE TABLE events
(
    created_at  TIMESTAMP DEFAULT now() NOT NULL,
    type        VARCHAR                 NOT NULL,
    payload     JSONB                   NOT NULL,
    id          UUID                    NOT NULL
        CONSTRAINT events_pkey
            PRIMARY KEY,
    creator     VARCHAR,
    stream_id   UUID                    NOT NULL,
    stream_type VARCHAR                 NOT NULL
);
