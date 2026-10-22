--liquibase formatted sql

--changeset your_name:create-notification-task-table
CREATE TABLE notification_task
(
    id                BIGSERIAL PRIMARY KEY,
    chat_id           BIGINT       NOT NULL,
    message           VARCHAR(255) NOT NULL,
    notification_time TIMESTAMP    NOT NULL
);