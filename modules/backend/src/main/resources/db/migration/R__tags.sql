CREATE TABLE tags_articles (
  article_id uuid NOT NULL,
  tag TEXT NOT NULL,
  FOREIGN KEY (article_id) REFERENCES articles (id) ON UPDATE CASCADE,
  PRIMARY KEY (article_id, tag)
);