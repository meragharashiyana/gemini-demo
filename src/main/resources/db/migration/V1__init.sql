-- Flyway migration: initial schema + seed data

CREATE TABLE greetings (
    id SERIAL PRIMARY KEY,
    message VARCHAR(255) NOT NULL
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL
);

INSERT INTO greetings (message) VALUES ('Hello from the database!');
INSERT INTO greetings (message) VALUES ('Database says hi!');
INSERT INTO greetings (message) VALUES ('Greetings from H2!');

INSERT INTO users (username, email) VALUES ('alice', 'alice@example.com');
INSERT INTO users (username, email) VALUES ('bob', 'bob@example.com');
