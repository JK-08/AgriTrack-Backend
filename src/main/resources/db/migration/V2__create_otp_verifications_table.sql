-- Forgot Password / MPIN recovery: OTP issuance + verification + one-time
-- reset tokens, all in one table shared by both purposes (PASSWORD_RESET,
-- MPIN_RESET) since the lifecycle is identical.

CREATE TABLE OTP_VERIFICATIONS (
    ID                       BIGINT IDENTITY(1,1) PRIMARY KEY,
    IDENTIFIER               VARCHAR(255) NOT NULL,   -- email or mobile number used to request the OTP
    PURPOSE                  VARCHAR(30)  NOT NULL,   -- PASSWORD_RESET / MPIN_RESET
    OTP_HASH                 VARCHAR(128) NOT NULL,
    EXPIRES_AT                DATETIME2 NOT NULL,
    USED                     BIT NOT NULL DEFAULT 0,  -- OTP itself has been successfully verified (consumed)
    ATTEMPT_COUNT            INT NOT NULL DEFAULT 0,
    MAX_ATTEMPTS             INT NOT NULL DEFAULT 5,
    VERIFIED                 BIT NOT NULL DEFAULT 0,
    VERIFIED_AT              DATETIME2 NULL,
    RESET_TOKEN_HASH         VARCHAR(128) NULL,
    RESET_TOKEN_EXPIRES_AT   DATETIME2 NULL,
    RESET_TOKEN_CONSUMED     BIT NOT NULL DEFAULT 0,
    CREATED_AT               DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX IX_OTP_VERIFICATIONS_IDENTIFIER_PURPOSE ON OTP_VERIFICATIONS (IDENTIFIER, PURPOSE, CREATED_AT);
