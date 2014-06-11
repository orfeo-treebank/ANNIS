﻿DROP TABLE IF EXISTS annotation_category CASCADE;

CREATE TABLE annotation_category
(
  id bigserial NOT NULL,
  namespace character varying,
  name character varying NOT NULL,
  toplevel_corpus integer NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (toplevel_corpus) REFERENCES corpus (id),
  UNIQUE (namespace, name)
);

DROP TABLE IF EXISTS annotation_category_50;

CREATE TABLE annotation_category_50
(
  PRIMARY KEY (id),
  FOREIGN KEY (toplevel_corpus) REFERENCES corpus (id),
  UNIQUE (namespace, name),
  CHECK (toplevel_corpus = 50)
)
INHERITS (annotation_category);

INSERT INTO annotation_category (toplevel_corpus, namespace, name)
(
  SELECT DISTINCT 50, namespace, name
  FROM annotations_50
  WHERE name IS NOT NULL  AND type = 'node'
);

-- DROP INDEX annocat_inverse_50;

CREATE INDEX annocat_inverse_50
  ON annotation_category_50
  USING btree
  (namespace, name, id);

