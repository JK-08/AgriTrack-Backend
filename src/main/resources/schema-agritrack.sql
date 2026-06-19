/* =====================================================================
   AgriTrack - SQL Server schema for the new modules
   (spring.jpa.hibernate.ddl-auto=none, so tables are created manually)
   Run once against database "users1". Existing USERS table is reused.
   ===================================================================== */

/* ---------- CUSTOMERS ---------- */
IF OBJECT_ID('dbo.CUSTOMERS', 'U') IS NULL
CREATE TABLE dbo.CUSTOMERS (
    CUSTOMER_ID               BIGINT IDENTITY(1,1) PRIMARY KEY,
    OWNER_ID                  BIGINT       NOT NULL,
    USER_ID                   BIGINT       NULL,
    NAME                      NVARCHAR(150) NOT NULL,
    MOBILE_NO                 NVARCHAR(15)  NULL,
    EMAIL                     NVARCHAR(150) NULL,
    ADDRESS                   NVARCHAR(255) NULL,
    VILLAGE                   NVARCHAR(150) NULL,
    CUSTOMER_TYPE             NVARCHAR(20)  NULL,
    FARM_SIZE                 NVARCHAR(50)  NULL,
    LATITUDE                  FLOAT         NULL,
    LONGITUDE                 FLOAT         NULL,
    PREFERRED_PAYMENT_METHOD  NVARCHAR(30)  NULL,
    PHOTO_URL                 NVARCHAR(500) NULL,
    NOTES                     NVARCHAR(500) NULL,
    CREATED_AT                DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- TRACTORS ---------- */
IF OBJECT_ID('dbo.TRACTORS', 'U') IS NULL
CREATE TABLE dbo.TRACTORS (
    TRACTOR_ID            BIGINT IDENTITY(1,1) PRIMARY KEY,
    OWNER_ID              BIGINT        NOT NULL,
    MODEL                 NVARCHAR(150) NULL,
    REGISTRATION_NUMBER   NVARCHAR(50)  NULL,
    MACHINE_TYPE          NVARCHAR(50)  NULL,
    CAPACITY              NVARCHAR(50)  NULL,
    HOURLY_RATE           DECIMAL(10,2) NULL,
    STATUS                NVARCHAR(20)  NULL,
    PHOTO_URL             NVARCHAR(500) NULL,
    CREATED_AT            DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- RATES ---------- */
IF OBJECT_ID('dbo.RATES', 'U') IS NULL
CREATE TABLE dbo.RATES (
    RATE_ID                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    OWNER_ID                BIGINT        NOT NULL,
    SERVICE_TYPE            NVARCHAR(100) NOT NULL,
    MACHINE_TYPE            NVARCHAR(50)  NULL,
    PRICE_PER_MINUTE        DECIMAL(10,2) NULL,
    PRICE_PER_TEN_MINUTES   DECIMAL(10,2) NULL,
    PRICE_PER_HOUR          DECIMAL(10,2) NULL,
    IS_ACTIVE               BIT           NOT NULL DEFAULT 1,
    CREATED_AT              DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- WORK_RECORDS ---------- */
IF OBJECT_ID('dbo.WORK_RECORDS', 'U') IS NULL
CREATE TABLE dbo.WORK_RECORDS (
    WORK_ID               BIGINT IDENTITY(1,1) PRIMARY KEY,
    OWNER_ID              BIGINT        NOT NULL,
    CUSTOMER_ID           BIGINT        NULL,
    TRACTOR_ID            BIGINT        NULL,
    RATE_ID               BIGINT        NULL,
    SERVICE_TYPE          NVARCHAR(100) NULL,
    WORK_DATE             DATE          NULL,
    START_TIME            DATETIME      NULL,
    END_TIME              DATETIME      NULL,
    LAST_RESUME_TIME      DATETIME      NULL,
    ACCUMULATED_SECONDS   BIGINT        NULL DEFAULT 0,
    DURATION_MINUTES      BIGINT        NULL,
    AMOUNT                DECIMAL(12,2) NULL,
    EXTRA_CHARGES         DECIMAL(12,2) NULL,
    STATUS                NVARCHAR(20)  NULL,
    NOTES                 NVARCHAR(500) NULL,
    CREATED_AT            DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- INVOICES ---------- */
IF OBJECT_ID('dbo.INVOICES', 'U') IS NULL
CREATE TABLE dbo.INVOICES (
    INVOICE_ID        BIGINT IDENTITY(1,1) PRIMARY KEY,
    INVOICE_NUMBER    NVARCHAR(50)  NULL,
    OWNER_ID          BIGINT        NOT NULL,
    CUSTOMER_ID       BIGINT        NULL,
    WORK_ID           BIGINT        NULL,
    BOOKING_ID        BIGINT        NULL,
    SUBTOTAL          DECIMAL(12,2) NULL,
    EXTRA_CHARGES     DECIMAL(12,2) NULL,
    TAX               DECIMAL(12,2) NULL,
    TOTAL_AMOUNT      DECIMAL(12,2) NULL,
    STATUS            NVARCHAR(20)  NULL,
    INVOICE_DATE      DATE          NULL,
    PDF_URL           NVARCHAR(500) NULL,
    NOTES             NVARCHAR(500) NULL,
    CREATED_AT        DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- PAYMENTS ---------- */
IF OBJECT_ID('dbo.PAYMENTS', 'U') IS NULL
CREATE TABLE dbo.PAYMENTS (
    PAYMENT_ID        BIGINT IDENTITY(1,1) PRIMARY KEY,
    INVOICE_ID        BIGINT        NULL,
    WORK_ID           BIGINT        NULL,
    BOOKING_ID        BIGINT        NULL,
    OWNER_ID          BIGINT        NULL,
    CUSTOMER_ID       BIGINT        NULL,
    AMOUNT            DECIMAL(12,2) NULL,
    PAYMENT_METHOD    NVARCHAR(30)  NULL,
    PAYMENT_STATUS    NVARCHAR(20)  NULL,
    TRANSACTION_ID    NVARCHAR(100) NULL,
    PAYMENT_DATE      DATETIME      NULL,
    NOTES             NVARCHAR(500) NULL,
    CREATED_AT        DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- BOOKINGS ---------- */
IF OBJECT_ID('dbo.BOOKINGS', 'U') IS NULL
CREATE TABLE dbo.BOOKINGS (
    BOOKING_ID        BIGINT IDENTITY(1,1) PRIMARY KEY,
    CLIENT_ID         BIGINT        NOT NULL,
    OWNER_ID          BIGINT        NOT NULL,
    TRACTOR_ID        BIGINT        NULL,
    SERVICE_TYPE      NVARCHAR(100) NULL,
    REQUESTED_DATE    DATETIME      NULL,
    FIELD_SIZE        NVARCHAR(50)  NULL,
    DURATION          NVARCHAR(50)  NULL,
    LOCATION          NVARCHAR(255) NULL,
    LATITUDE          FLOAT         NULL,
    LONGITUDE         FLOAT         NULL,
    AMOUNT            DECIMAL(12,2) NULL,
    STATUS            NVARCHAR(20)  NULL,
    NOTES             NVARCHAR(500) NULL,
    CREATED_AT        DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- MAINTENANCE_LOGS ---------- */
IF OBJECT_ID('dbo.MAINTENANCE_LOGS', 'U') IS NULL
CREATE TABLE dbo.MAINTENANCE_LOGS (
    MAINTENANCE_ID    BIGINT IDENTITY(1,1) PRIMARY KEY,
    TRACTOR_ID        BIGINT        NOT NULL,
    OWNER_ID          BIGINT        NULL,
    MAINTENANCE_TYPE  NVARCHAR(100) NULL,
    COST              DECIMAL(12,2) NULL,
    MAINTENANCE_DATE  DATE          NULL,
    NOTES             NVARCHAR(500) NULL,
    CREATED_AT        DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- CHATS ---------- */
IF OBJECT_ID('dbo.CHATS', 'U') IS NULL
CREATE TABLE dbo.CHATS (
    CHAT_ID            BIGINT IDENTITY(1,1) PRIMARY KEY,
    OWNER_ID           BIGINT        NOT NULL,
    CLIENT_ID          BIGINT        NOT NULL,
    LAST_MESSAGE       NVARCHAR(500) NULL,
    LAST_MESSAGE_TIME  DATETIME      NULL,
    CREATED_AT         DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- MESSAGES ---------- */
IF OBJECT_ID('dbo.MESSAGES', 'U') IS NULL
CREATE TABLE dbo.MESSAGES (
    MESSAGE_ID    BIGINT IDENTITY(1,1) PRIMARY KEY,
    CHAT_ID       BIGINT         NOT NULL,
    SENDER_ID     BIGINT         NOT NULL,
    MESSAGE_TEXT  NVARCHAR(1000) NULL,
    IS_READ       BIT            NOT NULL DEFAULT 0,
    CREATED_AT    DATETIME       NOT NULL DEFAULT GETDATE()
);

/* ---------- RATINGS ---------- */
IF OBJECT_ID('dbo.RATINGS', 'U') IS NULL
CREATE TABLE dbo.RATINGS (
    RATING_ID     BIGINT IDENTITY(1,1) PRIMARY KEY,
    BOOKING_ID    BIGINT         NULL,
    CLIENT_ID     BIGINT         NOT NULL,
    OWNER_ID      BIGINT         NOT NULL,
    RATING_VALUE  INT            NULL,
    REVIEW        NVARCHAR(1000) NULL,
    CREATED_AT    DATETIME       NOT NULL DEFAULT GETDATE()
);
