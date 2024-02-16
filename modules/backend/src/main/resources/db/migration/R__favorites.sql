CREATE TABLE favorites_articles (
  article_id uuid NOT NULL,
  user_id uuid NOT NULL,
  FOREIGN KEY (article_id) REFERENCES articles (id) ON UPDATE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users (id),
  PRIMARY KEY (article_id, user_id)
);