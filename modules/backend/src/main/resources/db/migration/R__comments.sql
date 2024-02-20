CREATE TABLE IF NOT EXISTS comments (
  id serial PRIMARY KEY,
  article_id UUID NOT NULL REFERENCES articles (id) ON DELETE CASCADE,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  author_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  body TEXT NOT NULL
)