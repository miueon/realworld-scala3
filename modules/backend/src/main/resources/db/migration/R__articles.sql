CREATE TABLE IF NOT EXISTS articles (
  id UUID PRIMARY KEY,
  slug TEXT NOT NULL UNIQUE,
  title TEXT NOT NULL UNIQUE,
  description TEXT NOT NULL,
  body TEXT NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  author_id UUID NOT NULL,
  FOREIGN KEY (author_id) REFERENCES users (id)
);