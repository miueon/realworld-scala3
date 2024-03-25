CREATE TABLE IF NOT EXISTS comments (
  id serial PRIMARY KEY,
  article_id UUID NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  author_id UUID NOT NULL,
  body TEXT NOT NULL
)