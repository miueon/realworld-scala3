CREATE TABLE IF NOT EXISTS tags_articles (
  article_id uuid NOT NULL,
  tag TEXT NOT NULL,
  PRIMARY KEY (article_id, tag)
);