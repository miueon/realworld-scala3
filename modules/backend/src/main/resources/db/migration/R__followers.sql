CREATE TABLE IF NOT EXISTS followers (
  user_id uuid NOT NULL,
  follower_id uuid NOT NULL,
  PRIMARY KEY (user_id, follower_id)
);