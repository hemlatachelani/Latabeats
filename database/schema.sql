-- LataBeats - MySQL schema.
-- The program creates all of this by itself on first start; this file is only for reference
-- (or if you prefer to prepare the database yourself).

CREATE DATABASE IF NOT EXISTS latabeats CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE latabeats;

-- One row per registered person. (Passwords are stored as plain text - no hashing.)
CREATE TABLE IF NOT EXISTS users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(30)  NOT NULL,
  email         VARCHAR(120) NOT NULL,
  password      VARCHAR(128) NOT NULL,
  display_name  VARCHAR(60)  NOT NULL,
  music_folder  VARCHAR(1024) NULL,             -- the folder the songs are read from
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login    TIMESTAMP NULL,
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Only the favourite's file path is stored. The songs themselves stay in the music folder.
CREATE TABLE IF NOT EXISTS favourite_songs (
  user_id   INT NOT NULL,
  song_path VARCHAR(700) NOT NULL,
  title     VARCHAR(255) NULL,
  artist    VARCHAR(255) NULL,
  added_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, song_path),
  CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Recommended: a dedicated MySQL user that can only touch this one database.
-- CREATE USER 'nocturne'@'localhost' IDENTIFIED BY 'choose-a-password';
-- GRANT ALL PRIVILEGES ON nocturne.* TO 'nocturne'@'localhost';
