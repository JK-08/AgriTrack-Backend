# AgriTrack Backend — API Reference (new modules)

Base URL: `http://<host>:8080`  ·  All routes are currently open (SecurityConfig permits all; JWT filter still populates auth).
Conventions follow the existing code: package `AgriTrackBackend.<MODULE>`, UPPERCASE tables/columns, plain `Long` FK columns.
Run `src/main/resources/schema-agritrack.sql` once (ddl-auto=none) to create the new tables.

Roles: OWNER, DRIVER, CUSTOMER.

## Customer  `/api/v1/customer`
- POST `/create` (body: Customer)
- GET  `/getAll`
- GET  `/getByOwner/{ownerId}`
- GET  `/search/{ownerId}?name=`
- GET  `/filter/{ownerId}?type=NEW|EXISTING`
- GET  `/getById/{id}`
- PUT  `/update/{id}`
- DELETE `/deleteById/{id}`

## Tractor / Machine  `/api/v1/tractor`
- POST `/create` · GET `/getAll` · GET `/getByOwner/{ownerId}` · GET `/available/{ownerId}`
- GET `/getById/{id}` · PUT `/update/{id}` · PUT `/status/{id}?status=` · DELETE `/deleteById/{id}`

## Rate  `/api/v1/rate`
- POST `/create` · GET `/getAll` · GET `/getByOwner/{ownerId}` · GET `/active/{ownerId}`
- GET `/getById/{id}` · PUT `/update/{id}` · DELETE `/deleteById/{id}`
- Rate fields: pricePerMinute, pricePerTenMinutes, pricePerHour.

## Work / Timer  `/api/v1/work`
- POST `/start` (body: {ownerId, customerId, tractorId, rateId, serviceType, notes}) → starts live timer
- PUT  `/pause/{id}` · PUT `/resume/{id}` · PUT `/stop/{id}?extraCharges=` → finalizes duration + amount from Rate
- POST `/manual` (body: WorkRecord with durationMinutes) → no live timer, amount auto-calculated
- GET  `/getByOwner/{ownerId}` · GET `/getByCustomer/{customerId}` · GET `/getById/{id}` · GET `/getAll`
- DELETE `/deleteById/{id}`

## Invoice  `/api/v1/invoice`
- POST `/create` (auto invoiceNumber + total = subtotal+extra+tax) · GET `/getAll`
- GET `/getByOwner/{ownerId}` · GET `/getByCustomer/{customerId}` · GET `/getById/{id}`
- PUT `/update/{id}` · PUT `/status/{id}?status=PAID|UNPAID|PARTIAL` · DELETE `/deleteById/{id}`

## Payment  `/api/v1/payment`
- POST `/create` · GET `/getAll` · GET `/history/{ownerId}` · GET `/customerHistory/{customerId}`
- GET `/pending/{ownerId}` · GET `/byInvoice/{invoiceId}` · GET `/getById/{id}`
- PUT `/status/{id}?status=PENDING|SUCCESS|FAILED` · DELETE `/deleteById/{id}`

## Booking (marketplace)  `/api/v1/booking`
- POST `/create` (client requests service) · GET `/getByOwner/{ownerId}` · GET `/getByClient/{clientId}` · GET `/pending/{ownerId}`
- PUT `/accept/{id}` · PUT `/reject/{id}` · PUT `/complete/{id}` · PUT `/cancel/{id}`
- GET `/getById/{id}` · GET `/getAll` · DELETE `/deleteById/{id}`

## Maintenance  `/api/v1/maintenance`
- POST `/create` · GET `/getByTractor/{tractorId}` · GET `/getByOwner/{ownerId}`
- GET `/getById/{id}` · PUT `/update/{id}` · DELETE `/deleteById/{id}`

## Chat  `/api/v1/chat`
- POST `/start?ownerId=&clientId=` (find/create conversation)
- POST `/send` (body: {ownerId, clientId, senderId, messageText})
- GET  `/owner/{ownerId}` · GET `/client/{clientId}` · GET `/messages/{chatId}`
- PUT  `/markRead/{chatId}?readerId=`

## Rating  `/api/v1/rating`
- POST `/create` · GET `/getByOwner/{ownerId}` · GET `/getByClient/{clientId}`
- GET `/average/{ownerId}` · GET `/getById/{id}` · GET `/getAll` · DELETE `/deleteById/{id}`

## Report / Analytics  `/api/v1/report`
- GET `/owner/{ownerId}` → {totalCustomers, totalWorks, completedWorks, totalBookings, pendingBookings, totalRevenue, pendingDue, averageRating}
- GET `/revenue/{ownerId}` → revenue grouped by work date (for charts)

## Existing modules (unchanged)
USERS `/api/v1/user`, MPIN `/api/v1/mpin`, ONBOARDING `/api/v1/onboarding`, NOTIFICATION, GOOGLEAUTH.
