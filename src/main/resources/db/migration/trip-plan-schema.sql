CREATE TABLE IF NOT EXISTS trip_request (
    id UUID PRIMARY KEY,
    destination VARCHAR(150) NOT NULL,
    days INTEGER NOT NULL,
    budget NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    travel_style VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    travelers INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS agent_execution_log (
    id UUID PRIMARY KEY,
    trip_request_id UUID NOT NULL,
    agent_name VARCHAR(100) NOT NULL,
    input_payload TEXT,
    output_payload TEXT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_agent_log_trip_request
        FOREIGN KEY (trip_request_id)
        REFERENCES trip_request(id)
);

CREATE INDEX IF NOT EXISTS idx_agent_execution_trip_request_id
ON agent_execution_log(trip_request_id);

CREATE INDEX IF NOT EXISTS idx_trip_request_created_at
ON trip_request(created_at);