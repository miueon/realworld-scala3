CREATE TABLE IF NOT EXISTS favorites_articles (
  article_id uuid NOT NULL,
  user_id uuid NOT NULL,
  PRIMARY KEY (article_id, user_id)
);