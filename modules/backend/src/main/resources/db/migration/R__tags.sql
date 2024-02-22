CREATE TABLE IF NOT EXISTS tags_articles (
  article_id uuid NOT NULL,
  tag TEXT NOT NULL,
  FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
  PRIMARY KEY (article_id, tag)
);