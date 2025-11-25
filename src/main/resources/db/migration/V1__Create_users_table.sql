-- Flyway migration script for users table
-- Version: 1
-- Description: Create users table with indexes

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Create indexes for performance optimization
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_active ON users(active);

-- Insert sample data for testing
INSERT INTO users (username, email, first_name, last_name, active) VALUES
    ('john_doe', 'john.doe@example.com', 'John', 'Doe', TRUE),
    ('jane_smith', 'jane.smith@example.com', 'Jane', 'Smith', TRUE),
    ('admin', 'admin@example.com', 'Admin', 'User', TRUE);
