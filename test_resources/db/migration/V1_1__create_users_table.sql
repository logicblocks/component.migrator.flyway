CREATE TABLE users
(
    id UUID NOT NULL
        CONSTRAINT users_pkey
            PRIMARY KEY,
    name VARCHAR NOT NULL
);
