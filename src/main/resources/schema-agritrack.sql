/* =====================================================================
   AgriTrack - Full SQL Server schema
   (spring.jpa.hibernate.ddl-auto=none, so tables are created manually)

   Run this ONCE against the new database "AGRITRACK"
   (server 103.91.218.31,1922) before starting the backend.
   Column names/types/lengths mirror the @Column annotations on the
   JPA entities exactly - do not rename without updating both sides.
   ===================================================================== */

/* ---------- USERS ---------- */
IF OBJECT_ID('dbo.USERS', 'U') IS NULL
CREATE TABLE dbo.USERS (
    USER_ID       BIGINT IDENTITY(1,1) PRIMARY KEY,
    NAME          NVARCHAR(255) NULL,
    MOBILE_NO     NVARCHAR(15)  NULL,
    EMAIL         NVARCHAR(255) NULL UNIQUE,
    PASSWORD      NVARCHAR(255) NULL,
    ROLE          NVARCHAR(255) NULL,
    CREATED_AT    DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- ONBOARDINGS ---------- */
IF OBJECT_ID('dbo.ONBOARDINGS', 'U') IS NULL
CREATE TABLE dbo.ONBOARDINGS (
    ONBOARDING_ID  BIGINT IDENTITY(1,1) PRIMARY KEY,
    TITLE          NVARCHAR(150) NOT NULL,
    SUBTITLE       NVARCHAR(255) NULL,
    IMAGE_URL      NVARCHAR(500) NULL,
    CREATED_AT     DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- MPINS ---------- */
IF OBJECT_ID('dbo.MPINS', 'U') IS NULL
CREATE TABLE dbo.MPINS (
    MPIN_ID     BIGINT IDENTITY(1,1) PRIMARY KEY,
    USER_ID     BIGINT        NOT NULL UNIQUE
        REFERENCES dbo.USERS(USER_ID),
    MPIN        NVARCHAR(255) NULL,
    CREATED_AT  DATETIME      NOT NULL DEFAULT GETDATE()
);

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

/* ---------- NOTIFICATIONS ---------- */
IF OBJECT_ID('dbo.NOTIFICATIONS', 'U') IS NULL
CREATE TABLE dbo.NOTIFICATIONS (
    NOTIFICATION_ID     BIGINT IDENTITY(1,1) PRIMARY KEY,
    USER_ID             BIGINT        NULL,
    TITLE               NVARCHAR(255) NULL,
    SUBTITLE            NVARCHAR(500) NULL,
    IMAGE_URL           NVARCHAR(1000) NULL,
    SCREEN_NAME         NVARCHAR(255) NULL,
    TIMER_SECONDS       INT           NULL,
    NOTIFICATION_TYPE   NVARCHAR(255) NULL,
    CLICK_ACTION        NVARCHAR(255) NULL,
    IS_ACTIVE           BIT           NOT NULL DEFAULT 1,
    IS_SENT             BIT           NOT NULL DEFAULT 0,
    SEND_AT             DATETIME      NULL,
    CREATED_AT          DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- USER_NOTIFICATION_TOKENS ---------- */
IF OBJECT_ID('dbo.USER_NOTIFICATION_TOKENS', 'U') IS NULL
CREATE TABLE dbo.USER_NOTIFICATION_TOKENS (
    TOKEN_ID      BIGINT IDENTITY(1,1) PRIMARY KEY,
    USER_ID       BIGINT        NULL,
    FCM_TOKEN     NVARCHAR(500) NOT NULL UNIQUE,
    DEVICE_TYPE   NVARCHAR(255) NULL,
    IS_ACTIVE     BIT           NOT NULL DEFAULT 1,
    CREATED_AT    DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- DRIVERS ----------
   Profile for a USERS row with ROLE = 'DRIVER'; belongs to one OWNER. */
IF OBJECT_ID('dbo.DRIVERS', 'U') IS NULL
CREATE TABLE dbo.DRIVERS (
    DRIVER_ID         BIGINT IDENTITY(1,1) PRIMARY KEY,
    USER_ID           BIGINT        NOT NULL UNIQUE,
    OWNER_ID          BIGINT        NOT NULL,
    LICENSE_NUMBER    NVARCHAR(50)  NULL,
    LICENSE_EXPIRY    DATE          NULL,
    PHOTO_URL         NVARCHAR(500) NULL,
    STATUS            NVARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    IS_AVAILABLE      BIT           NOT NULL DEFAULT 1,
    NOTES             NVARCHAR(500) NULL,
    CREATED_AT        DATETIME      NOT NULL DEFAULT GETDATE()
);

/* ---------- TRACTOR_DRIVER_ASSIGNMENTS ----------
   Which driver currently operates which tractor for a given owner. */
IF OBJECT_ID('dbo.TRACTOR_DRIVER_ASSIGNMENTS', 'U') IS NULL
CREATE TABLE dbo.TRACTOR_DRIVER_ASSIGNMENTS (
    ASSIGNMENT_ID   BIGINT IDENTITY(1,1) PRIMARY KEY,
    OWNER_ID        BIGINT       NOT NULL,
    TRACTOR_ID      BIGINT       NOT NULL,
    DRIVER_ID       BIGINT       NOT NULL,
    IS_ACTIVE       BIT          NOT NULL DEFAULT 1,
    ASSIGNED_AT     DATETIME     NOT NULL DEFAULT GETDATE(),
    UNASSIGNED_AT   DATETIME     NULL,
    CREATED_AT      DATETIME     NOT NULL DEFAULT GETDATE()
);

/* ---------- BOOKINGS.DRIVER_ID / WORK_RECORDS.DRIVER_ID ----------
   The driver assigned to fulfil a booking / work session. */
IF COL_LENGTH('dbo.BOOKINGS', 'DRIVER_ID') IS NULL
ALTER TABLE dbo.BOOKINGS ADD DRIVER_ID BIGINT NULL;

IF COL_LENGTH('dbo.WORK_RECORDS', 'DRIVER_ID') IS NULL
ALTER TABLE dbo.WORK_RECORDS ADD DRIVER_ID BIGINT NULL;

IF COL_LENGTH('dbo.WORK_RECORDS', 'BOOKING_ID') IS NULL
ALTER TABLE dbo.WORK_RECORDS ADD BOOKING_ID BIGINT NULL;

/* ---------- DRIVER_LOCATIONS ----------
   Live GPS breadcrumbs posted by the driver app while a job is running. */
IF OBJECT_ID('dbo.DRIVER_LOCATIONS', 'U') IS NULL
CREATE TABLE dbo.DRIVER_LOCATIONS (
    LOCATION_ID   BIGINT IDENTITY(1,1) PRIMARY KEY,
    DRIVER_ID     BIGINT   NOT NULL,
    BOOKING_ID    BIGINT   NULL,
    WORK_ID       BIGINT   NULL,
    LATITUDE      FLOAT    NOT NULL,
    LONGITUDE     FLOAT    NOT NULL,
    RECORDED_AT   DATETIME NOT NULL DEFAULT GETDATE()
);
