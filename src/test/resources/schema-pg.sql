DROP TABLE IF EXISTS ms_user;
CREATE TABLE ms_user (
    id     BIGINT       PRIMARY KEY,
    name   VARCHAR(64)  NOT NULL,
    age    INTEGER,
    active BOOLEAN
);
