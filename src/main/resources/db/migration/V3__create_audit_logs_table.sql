-- Activity/audit trail: who did what, to which entity, and (where available)
-- what the record looked like before and after the change.

CREATE TABLE AUDIT_LOGS (
    ID           BIGINT IDENTITY(1,1) PRIMARY KEY,
    USER_ID      BIGINT NULL,          -- null for unauthenticated/system-triggered actions
    ACTION       VARCHAR(50)  NOT NULL, -- CREATE / UPDATE / DELETE / STATUS_CHANGE / LOGIN / LOGOUT / ...
    ENTITY_TYPE  VARCHAR(100) NOT NULL, -- e.g. "Customer", "Booking"
    ENTITY_ID    VARCHAR(50)  NULL,
    BEFORE_VALUE NVARCHAR(MAX) NULL,    -- JSON snapshot before the change
    AFTER_VALUE  NVARCHAR(MAX) NULL,    -- JSON snapshot after the change
    IP_ADDRESS   VARCHAR(64)  NULL,
    CREATED_AT   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX IX_AUDIT_LOGS_ENTITY ON AUDIT_LOGS (ENTITY_TYPE, ENTITY_ID);
CREATE INDEX IX_AUDIT_LOGS_USER ON AUDIT_LOGS (USER_ID, CREATED_AT);
