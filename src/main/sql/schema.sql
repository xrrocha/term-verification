CREATE TABLE terms (
    term_id             VARCHAR(32)     NOT NULL    PRIMARY KEY,
    term_type           CHAR(1)                     CHECK(term_type IN ('x', 'n', 'a', 't', 'm', 'u')),
    -- typo
    base_term_id        VARCHAR(32), -- TRANSLATE(base_term_id, 'áéíóú', 'aeiou') REFERENCES terms
    last_modified       TIMESTAMP,
    user_modified       VARCHAR(16)
);

CREATE TABLE term_usages (
    term_id             VARCHAR(32)     NOT NULL    REFERENCES terms, -- term_type: n
    usage               CHAR(1)         NOT NULL    CHECK(usage IN ('f', 'F', 'm', 'M', 's')),
    PRIMARY KEY(term_id, usage),
    base_term_id        VARCHAR(32) -- TRANSLATE(base_term_id, 'áéíóú', 'aeiou') REFERENCES terms
);

CREATE TABLE term_similarities (
    term_1              VARCHAR(32)     NOT NULL    REFERENCES terms,
    term_2              VARCHAR(32)     NOT NULL    REFERENCES terms,
    PRIMARY KEY(term_1, term_2),
    levenshtein          NUMERIC(9, 8)   NOT NULL    CHECK(levenshtein BETWEEN 0 AND 1)
);

CREATE INDEX ts_term2 ON term_similarities(term2);

CREATE TABLE term_names (
    term_id             VARCHAR(30)     NOT NULL    REFERENCES terms,
    name                VARCHAR(64)     NOT NULL,
    PRIMARY KEY(term_id, name)
);
