CREATE TABLE IF NOT EXISTS followers (
  user_id uuid NOT NULL REFERENCES users (id),
  follower_id uuid NOT NULL REFERENCES users (id),
  PRIMARY KEY (user_id, follower_id)
);