-- Task 6 - Campus Event Registration - MySQL 8 schema
-- mysql -u root -p < sql/schema.sql
CREATE DATABASE IF NOT EXISTS campus_events CHARACTER SET utf8mb4;
USE campus_events;

CREATE TABLE IF NOT EXISTS events (
  event_id    INT          PRIMARY KEY,
  name        VARCHAR(80)  NOT NULL,
  event_date  DATE         NOT NULL,
  venue       VARCHAR(80)  NOT NULL,
  capacity    INT          NOT NULL CHECK (capacity > 0)
);

CREATE TABLE IF NOT EXISTS registrations (
  reg_id        BIGINT       AUTO_INCREMENT PRIMARY KEY,
  reg_no        CHAR(9)      NOT NULL,
  student_name  VARCHAR(60)  NOT NULL,
  email         VARCHAR(120) NOT NULL,
  dept          VARCHAR(10)  NOT NULL,
  year          TINYINT      NOT NULL CHECK (year BETWEEN 1 AND 4),
  event_id      INT          NOT NULL,
  registered_at DATETIME     NOT NULL,
  CONSTRAINT fk_reg_event     FOREIGN KEY (event_id) REFERENCES events(event_id),
  CONSTRAINT uq_student_event UNIQUE (reg_no, event_id),      -- rule R7: no double registration
  INDEX idx_reg_event (event_id, reg_id)                       -- filter + paging
);

INSERT INTO events (event_id, name, event_date, venue, capacity) VALUES (1, 'Hackathon 24h', '2026-12-02', 'Main Auditorium', 120);
INSERT INTO events (event_id, name, event_date, venue, capacity) VALUES (2, 'Classical Dance', '2026-12-03', 'Open Air Theatre', 60);
INSERT INTO events (event_id, name, event_date, venue, capacity) VALUES (3, 'Inter-college Debate', '2026-12-04', 'Seminar Hall 2', 40);
INSERT INTO events (event_id, name, event_date, venue, capacity) VALUES (4, 'Photography Walk', '2026-12-05', 'Campus Grounds', 3);
INSERT INTO events (event_id, name, event_date, venue, capacity) VALUES (5, 'Battle of Bands', '2026-12-06', 'Main Auditorium', 200);
INSERT INTO events (event_id, name, event_date, venue, capacity) VALUES (6, 'Tech Quiz', '2026-12-07', 'Seminar Hall 1', 80);
