CREATE TABLE users (
  id UUID PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  username TEXT NOT NULL UNIQUE,
  password TEXT NOT NULL,
  bio TEXT,
  image TEXT
);

-- CREATE TABLE tags_articles (
--   tag TEXT NOT NULL,
--   article_id INTEGER NOT NULL,
--   FOREIGN KEY (article_id) REFERENCES articles (article_id) ON UPDATE CASCADE,
--   PRIMARY KEY (tag, article_id)
-- );
-- CREATE TABLE favorites_articles (
--   profile_id INTEGER NOT NULL,
--   article_id INTEGER NOT NULL,
--   FOREIGN KEY (article_id) REFERENCES articles (article_id) ON UPDATE CASCADE,
--   FOREIGN KEY (profile_id) REFERENCES users (user_id),
--   PRIMARY KEY (profile_id, article_id)
-- );
-- CREATE TABLE comments_articles (
--   comment_id INTEGER PRIMARY KEY,
--   article_id INTEGER NOT NULL REFERENCES articles (article_id) ON UPDATE CASCADE,
--   created_at INTEGER NOT NULL,
--   updated_at INTEGER NOT NULL,
--   author_id INTEGER NOT NULL REFERENCES users (user_id),
--   body TEXT NOT NULL
-- )