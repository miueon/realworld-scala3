CREATE TABLE IF NOT EXISTS comments (
  id INTEGER PRIMARY KEY,
  article_id UUID NOT NULL REFERENCES articles (id) ON UPDATE CASCADE,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  author_id UUID NOT NULL REFERENCES users (id),
  body TEXT NOT NULL
)