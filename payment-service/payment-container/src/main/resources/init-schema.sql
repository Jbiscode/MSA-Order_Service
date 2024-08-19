DROP SCHEMA IF EXISTS payment CASCADE;

CREATE SCHEMA payment;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--
DROP TYPE IF EXISTS payment_status CASCADE;
CREATE TYPE payment_status AS ENUM (
    'COMPLETED',
    'CANCELLED',
    'FAILED'
    );

--
DROP TABLE IF EXISTS "payment".payments CASCADE;
CREATE TABLE "payment".payments
(
    id          UUID                     NOT NULL PRIMARY KEY,
    customer_id UUID                     NOT NULL,
    order_id    UUID                     NOT NULL,
    price       NUMERIC(10, 2)           NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    status      payment_status           NOT NULL
);

--
DROP TABLE IF EXISTS "payment".credit_entry CASCADE;
CREATE TABLE "payment".credit_entry
(
    id                  UUID           NOT NULL PRIMARY KEY,
    customer_id         UUID           NOT NULL,
    total_credit_amount NUMERIC(10, 2) NOT NULL
);

--
DROP TYPE IF EXISTS transaction_type CASCADE;
CREATE TYPE transaction_type AS ENUM (
    'CREDIT',
    'DEBIT'
    );

--
DROP TABLE IF EXISTS "payment".credit_history CASCADE;
CREATE TABLE "payment".credit_history
(
    id          UUID             NOT NULL PRIMARY KEY,
    customer_id UUID             NOT NULL,
    amount      NUMERIC(10, 2)   NOT NULL,
    type        transaction_type NOT NULL
);

DROP TYPE IF EXISTS outbox_status CASCADE;
CREATE TYPE outbox_status AS ENUM ('STARTED', 'COMPLETED', 'FAILED');

DROP TABLE IF EXISTS "payment".order_outbox CASCADE;
CREATE TABLE "payment".order_outbox
(
    id             UUID                                           NOT NULL PRIMARY KEY,
    saga_id        UUID                                           NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE                       NOT NULL,
    processed_at   TIMESTAMP WITH TIME ZONE,
    type           CHARACTER VARYING COLLATE pg_catalog."default" NOT NULL,
    payload        jsonb                                          NOT NULL,
    outbox_status  outbox_status                                  NOT NULL,
    payment_status payment_status                                 NOT NULL,
    version        INTEGER                                        NOT NULL
);

CREATE INDEX "payment_order_outbox_saga_status"
    ON "payment".order_outbox 
    (type, outbox_status);

CREATE UNIQUE INDEX "payment_order_outbox_saga_id_payment_status_outbox_status"
    ON "payment".order_outbox
    (type, saga_id, payment_status, outbox_status);