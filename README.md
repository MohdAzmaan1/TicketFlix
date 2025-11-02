# Book My Show (TicketFlix)

## Table of Contents

1. [Technology Used](#technology-used)
2. [Architecture Overview](#architecture-overview)
3. [EER Diagram](#eer-diagram)
4. [Functionalities](#functionalities)
5. [Authentication & Authorization](#authentication--authorization)
6. [API Documentation](#api-documentation)
7. [Kafka Integration](#kafka-integration)
8. [Redis Integration](#redis-integration)
9. [Payment Flow](#payment-flow)
10. [Setup Instructions](#setup-instructions)
11. [Future Scope](#future-scope)

---

## Technology Used

- **Java** 17
- **Spring Boot** 2.7.8
- **Spring Data JPA** / **Hibernate**
- **RESTful APIs**
- **Maven**
- **MySQL** (via Aiven Cloud)
- **Redis** (for caching and distributed locks)
- **Apache Kafka** (for event-driven architecture)
- **Redisson** (for distributed locking)
- **JavaMail** (for email notifications)
- **Lombok** (for boilerplate code reduction)
- **Swagger/OpenAPI** (for API documentation)
- **Spring Security** (for authentication and authorization)
- **JWT (JSON Web Tokens)** (for stateless authentication)
- **BCrypt** (for password encryption)

---

## Architecture Overview

### System Architecture

```
┌─────────────┐
│   Client    │
│  (Browser/  │
│   Mobile)   │
└──────┬──────┘
       │ HTTP/REST
       ▼
┌─────────────────────────────────────────┐
│         REST Controllers                 │
│  (User, Movie, Theater, Show, Ticket)   │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│         Service Layer                    │
│  (Business Logic + Validation)          │
└──────┬──────────────────────────────────┘
       │
       ├─────────────────┬──────────────────┐
       ▼                 ▼                  ▼
┌──────────────┐  ┌─────────────┐  ┌──────────────┐
│   Kafka      │  │    Redis    │  │   MySQL DB   │
│  (Events)    │  │  (Cache +   │  │ (Persistence)│
│              │  │   Locks)    │  │              │
└──────┬───────┘  └─────────────┘  └──────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│      Kafka Consumers                    │
│  (Async Processing + DB Persistence)    │
└─────────────────────────────────────────┘
```

### Data Flow Pattern

**Create/Update/Delete Operations:**
```
Controller → Service → Kafka Producer → Kafka Topic → Kafka Consumer → Database
```

**Read Operations:**
```
Controller → Service → Redis (Cache Check) → [Cache Hit] → Return
                                        ↓ [Cache Miss]
                                   Database → Cache → Return
```

---

## EER Diagram

![Schema](https://user-images.githubusercontent.com/116377954/222906823-f7682629-2383-496b-91a0-923bcedd9b00.png)

---

## Functionalities

### User Management
- ✅ Register User with Password (with Kafka event)
- ✅ User Login (JWT token generation)
- ✅ Get All Users (authenticated)
- ✅ Get User by ID (authenticated)
- ✅ Update User Profile (authenticated, with Kafka event)
- ✅ Delete User (Admin only, with Kafka event)
- ✅ Email uniqueness validation
- ✅ Password encryption (BCrypt)
- ✅ Role-based access control (USER, ADMIN, THEATER_OWNER)

### Theater Management
- ✅ Add Theater (Admin/Theater Owner, with Kafka event)
- ✅ Multiple Screens per Theater
- ✅ Get All Theaters
- ✅ Get Theater by ID (with Redis caching)
- ✅ Update Theater (Admin/Theater Owner, with Kafka event)
- ✅ Delete Theater (Admin/Theater Owner, with Kafka event)
- ✅ Theater seat management via screens

### Movie Management
- ✅ Add Movie (with Kafka event)
- ✅ Get All Movies
- ✅ Get Movie by ID (with Redis caching)
- ✅ Update Movie (with Kafka event)
- ✅ Delete Movie (with Kafka event)
- ✅ Get Trending Movies (Redis ZSet)
- ✅ Get Trending Movies with Counts

### Show Management
- ✅ Add Show (Admin/Theater Owner, with Kafka event, requires Screen ID)
- ✅ Get All Shows
- ✅ Get Show by ID (with Redis caching)
- ✅ Get Shows by Movie
- ✅ Get Shows by Theater
- ✅ Update Show (Admin/Theater Owner, with Kafka event)
- ✅ Delete Show (Admin/Theater Owner, with Kafka event)
- ✅ Screen-based show scheduling

### Ticket Management
- ✅ Book Tickets (authenticated, with payment flow, Kafka event + distributed locking)
- ✅ Cancel Tickets (authenticated, with Kafka event)
- ✅ Get All Tickets (authenticated)
- ✅ Get Ticket by ID (authenticated, with Redis caching)
- ✅ Get Ticket by Ticket ID (UUID)
- ✅ Get Tickets by User (authenticated, with Redis caching)
- ✅ Get Tickets by Show (authenticated, with Redis caching)
- ✅ Seat locking mechanism (Redisson)
- ✅ Email notifications (async via Kafka)
- ✅ Trending movies tracking

### Payment Management
- ✅ Process Payment (with idempotency key)
- ✅ Payment Hash Verification
- ✅ Get Payment Status
- ✅ Dummy Payment Gateway (90% success rate)
- ✅ Automatic ticket booking on successful payment

### Screen Management
- ✅ Add Screen to Theater (Admin/Theater Owner)
- ✅ Get All Screens
- ✅ Get Screen by ID
- ✅ Get Screens by Theater
- ✅ Update Screen (Admin/Theater Owner)
- ✅ Delete Screen (Admin/Theater Owner, validation for scheduled shows)

---

## Authentication & Authorization

### Overview

The application uses **JWT (JSON Web Tokens)** for stateless authentication and **Spring Security** for authorization with role-based access control.

### User Roles

| Role | Description | Access Level |
|------|-------------|--------------|
| `USER` | Regular customer | Book tickets, view profile, manage own account |
| `ADMIN` | System administrator | Full access to all endpoints |
| `THEATER_OWNER` | Theater owner | Manage theaters, screens, shows, and movies |

### Authentication Flow

```
1. Register/Login → Get JWT Token
2. Store Token (client-side)
3. Include Token in Authorization Header
4. JWT Filter validates token
5. Security Context sets authentication
6. @PreAuthorize checks role permissions
```

### Step-by-Step Guide

#### Step 1: Register a New User

**POST** `localhost:8080/auth/register`

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "age": 25,
  "mobileNumber": "1234567890",
  "address": "123 Main St",
  "role": "USER"
}
```

**Response:** `201 CREATED`
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwidXNlcklkIjoxLCJyb2xlIjoiVVNFUiIsImlhdCI6MTY4OTk2MDAwMCwiZXhwIjoxNjkwMDQ2NDAwfQ...",
  "email": "john@example.com",
  "userId": 1,
  "name": "John Doe",
  "role": "USER",
  "message": "Registration successful"
}
```

**Important:** Save the `token` from the response. You'll need it for authenticated requests.

---

#### Step 2: Login (For Existing Users)

**POST** `localhost:8080/auth/login`

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwidXNlcklkIjoxLCJyb2xlIjoiVVNFUiIsImlhdCI6MTY4OTk2MDAwMCwiZXhwIjoxNjkwMDQ2NDAwfQ...",
  "email": "john@example.com",
  "userId": 1,
  "name": "John Doe",
  "role": "USER",
  "message": "Login successful"
}
```

**Important:** Save the `token` from the response.

---

#### Step 3: Access Protected APIs

After registration or login, include the JWT token in the `Authorization` header for all protected endpoints.

**Header Format:**
```
Authorization: Bearer <your-jwt-token>
```

**Example: Get User Profile**
```bash
curl -X GET http://localhost:8080/account/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Example: Book a Ticket**
```bash
curl -X POST http://localhost:8080/tickets/book \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "showId": 1,
    "userId": 1,
    "requestedSeats": ["1A", "1B"]
  }'
```

---

### Authentication APIs

#### 1. Register User
**POST** `localhost:8080/auth/register`

**Public Endpoint** - No authentication required

**Request Body:**
```json
{
  "name": "String",
  "email": "String (unique)",
  "password": "String (min 6 characters)",
  "age": 25,
  "mobileNumber": "String",
  "address": "String",
  "role": "USER" // Optional: USER, ADMIN, THEATER_OWNER (defaults to USER)
}
```

**Response:** `201 CREATED`
```json
{
  "token": "JWT_TOKEN",
  "email": "user@example.com",
  "userId": 1,
  "name": "User Name",
  "role": "USER",
  "message": "Registration successful"
}
```

---

#### 2. Login
**POST** `localhost:8080/auth/login`

**Public Endpoint** - No authentication required

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "token": "JWT_TOKEN",
  "email": "user@example.com",
  "userId": 1,
  "name": "User Name",
  "role": "USER",
  "message": "Login successful"
}
```

**Error Response:** `401 UNAUTHORIZED`
```json
{
  "message": "Login failed: Invalid email or password"
}
```

---

#### 3. Validate Token
**GET** `localhost:8080/auth/validate`

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** `200 OK`
```
"Token is valid"
```

---

### User Account Management APIs

#### 1. Get Profile
**GET** `localhost:8080/account/profile`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "age": 25,
  "mobileNumber": "1234567890",
  "address": "123 Main St"
}
```

---

#### 2. Update Profile
**PUT** `localhost:8080/account/profile`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "John Updated",
  "age": 26,
  "mobileNumber": "9876543210",
  "address": "456 New St"
}
```

**Response:** `200 OK`
```
"Profile update request submitted successfully"
```

---

#### 3. Change Password
**PUT** `localhost:8080/account/change-password`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "oldPassword": "password123",
  "newPassword": "newpassword456"
}
```

**Response:** `200 OK`
```
"Password change request submitted successfully"
```

---

### Endpoint Access Matrix

| Endpoint | USER | ADMIN | THEATER_OWNER | Public |
|----------|------|-------|---------------|--------|
| `/auth/**` | ✅ | ✅ | ✅ | ✅ |
| `/user/add` | ✅ | ✅ | ✅ | ✅ |
| `/user/get/**` | ✅ | ✅ | ✅ | ❌ |
| `/user/update/**` | ❌ | ✅ | ❌ | ❌ |
| `/user/delete/**` | ❌ | ✅ | ❌ | ❌ |
| `/movies/add` | ❌ | ✅ | ✅ | ❌ |
| `/movies/get/**` | ✅ | ✅ | ✅ | ✅ |
| `/theater/**` (CUD) | ❌ | ✅ | ✅ | ❌ |
| `/theater/**` (GET) | ✅ | ✅ | ✅ | ✅ |
| `/screens/**` (CUD) | ❌ | ✅ | ✅ | ❌ |
| `/screens/**` (GET) | ✅ | ✅ | ✅ | ✅ |
| `/shows/**` (CUD) | ❌ | ✅ | ✅ | ❌ |
| `/shows/**` (GET) | ✅ | ✅ | ✅ | ✅ |
| `/tickets/**` | ✅ | ✅ | ✅ | ❌ |
| `/payments/**` | ✅ | ✅ | ✅ | ❌ |
| `/account/**` | ✅ | ✅ | ✅ | ❌ |

**Legend:**
- ✅ = Allowed
- ❌ = Not Allowed
- CUD = Create, Update, Delete
- GET = Read operations

---

### Quick Start Guide

#### For New Users:

1. **Register:**
   ```bash
   curl -X POST http://localhost:8080/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "name": "John Doe",
       "email": "john@example.com",
       "password": "password123",
       "age": 25
     }'
   ```

2. **Save the token** from the response

3. **Use token for authenticated requests:**
   ```bash
   curl -X GET http://localhost:8080/account/profile \
     -H "Authorization: Bearer <your-token>"
   ```

#### For Existing Users:

1. **Login:**
   ```bash
   curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "email": "john@example.com",
       "password": "password123"
     }'
   ```

2. **Save the token** from the response

3. **Use token for authenticated requests:**
   ```bash
   curl -X POST http://localhost:8080/tickets/book \
     -H "Authorization: Bearer <your-token>" \
     -H "Content-Type: application/json" \
     -d '{
       "showId": 1,
       "userId": 1,
       "requestedSeats": ["1A", "1B"]
     }'
   ```

---

### Token Details

- **Algorithm:** HS512
- **Expiration:** 24 hours
- **Contains:** Email, User ID, Role
- **Validation:** Automatic on each request via JWT Filter

### Password Requirements

- **Minimum Length:** 6 characters
- **Encryption:** BCrypt (automatic)
- **Storage:** Encrypted in database

---

## API Documentation

### User APIs

#### 1. Add User (Registration)
**POST** `localhost:8080/user/add` (Public)  
**OR**  
**POST** `localhost:8080/auth/register` (Recommended - includes password and returns JWT token)

**Request Body:**
```json
{
  "name": "String",
  "email": "String (unique)",
  "password": "String (min 6 characters)",
  "address": "String",
  "mobileNumber": "String",
  "age": 25,
  "role": "USER" // Optional, defaults to USER
}
```

**Response:** `201 CREATED`
```json
{
  "token": "JWT_TOKEN_HERE",
  "email": "user@example.com",
  "userId": 1,
  "name": "User Name",
  "role": "USER",
  "message": "Registration successful"
}
```

**Note:** 
- `/auth/register` returns JWT token immediately (recommended)
- `/user/add` uses Kafka for async persistence (legacy, no password)
- Email must be unique

---

#### 2. Get All Users
**GET** `localhost:8080/user/get-all`

**Headers:** `Authorization: Bearer <token>` (Authenticated)

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "mobileNumber": "1234567890",
    "age": 25
  }
]
```

---

#### 3. Get User by ID
**GET** `localhost:8080/user/get/{userId}`

**Headers:** `Authorization: Bearer <token>` (Authenticated)

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "address": "123 Main St",
  "mobileNumber": "1234567890",
  "age": 25
}
```

---

#### 4. Update User
**PUT** `localhost:8080/user/update/{userId}`

**Headers:** `Authorization: Bearer <token>` (Admin only)

**Request Body:**
```json
	{
      "name": "String",
      "email": "String",
      "address": "String",
      "mobileNumber": "String",
  "age": 25,
  "password": "String" // Optional, will be encrypted
}
```

**Response:** `200 OK`
```json
"User update request submitted successfully"
```

**Note:** 
- Admin role required
- For self-updates, use `/account/profile`
- Uses Kafka for async persistence

---

#### 5. Delete User
**DELETE** `localhost:8080/user/delete/{userId}`

**Headers:** `Authorization: Bearer <token>` (Admin only)

**Response:** `200 OK`
```json
"User deletion request submitted successfully"
```

**Note:** 
- Admin role required
- Uses Kafka for async persistence

---

### Theater APIs

#### 1. Add Theater
**POST** `localhost:8080/theater/add`

**Headers:** `Authorization: Bearer <token>` (Admin or Theater Owner)

**Request Body:**
```json
{
  "name": "PVR Cinemas",
  "location": "Mumbai"
}
```

**Note:** After creating theater, add screens using `/screens/add` endpoint.

**Response:** `201 CREATED`
```json
"Theater creation request submitted successfully. Please add screens using /screens/add endpoint."
```

---

#### 2. Get All Theaters
**GET** `localhost:8080/theater/get-all`

**Response:** `200 OK`
```json
[
  {
    "name": "PVR Cinemas",
    "location": "Mumbai",
    "classicSeatsCount": 50,
    "premiumSeatsCount": 30
  }
]
```

---

#### 3. Get Theater by ID
**GET** `localhost:8080/theater/get/{theaterId}`

**Response:** `200 OK` (with Redis caching)

---

#### 4. Update Theater
**PUT** `localhost:8080/theater/update/{theaterId}`

**Request Body:** Same as Add Theater

---

#### 5. Delete Theater
**DELETE** `localhost:8080/theater/delete/{theaterId}`

---

### Movie APIs

#### 1. Add Movie
**POST** `localhost:8080/movies/add`

**Request Body:**
```json
{
  "movieName": "Inception",
  "genre": "SCI_FI",
  "language": "ENGLISH",
  "rating": 8.8,
  "duration": 148
}
```

**Available Genres:** `ACTION`, `COMEDY`, `DRAMA`, `HORROR`, `ROMANCE`, `SCI_FI`, `THRILLER`

**Available Languages:** `HINDI`, `ENGLISH`, `TELUGU`, `TAMIL`, `MALAYALAM`, `KANNADA`

**Response:** `201 CREATED`
```json
"Movie creation request submitted successfully"
```

---

#### 2. Get All Movies
**GET** `localhost:8080/movies/get-all`

---

#### 3. Get Movie by ID
**GET** `localhost:8080/movies/get/{movieId}`

**Response:** `200 OK` (with Redis caching)

---

#### 4. Update Movie
**PUT** `localhost:8080/movies/update/{movieId}`

**Request Body:** Same as Add Movie

---

#### 5. Delete Movie
**DELETE** `localhost:8080/movies/delete/{movieId}`

---

#### 6. Get Trending Movies
**GET** `localhost:8080/movies/trending?limit=10`

**Response:** `200 OK`
```json
["Inception", "Interstellar", "The Dark Knight"]
```

---

#### 7. Get Trending Movies with Counts
**GET** `localhost:8080/movies/trending-with-counts?limit=10`

**Response:** `200 OK`
```json
{
  "Inception": 150,
  "Interstellar": 120,
  "The Dark Knight": 100
}
```

---

### Show APIs

#### 1. Add Show
**POST** `localhost:8080/shows/add`

**Headers:** `Authorization: Bearer <token>` (Admin or Theater Owner)

**Request Body:**
```json
{
  "movieId": 1,
  "theaterId": 1,
  "screenId": 1,
  "classSeatPrice": 300,
  "premiumSeatPrice": 500,
  "showType": "2D",
  "showTime": "14:30:00",
  "showDate": "2024-01-15"
}
```

**Available Show Types:** `2D`, `3D`, `IMAX`, `4DX`

**Note:** `screenId` is required - shows are scheduled on specific screens.

**Response:** `201 CREATED`

---

#### 2. Get All Shows
**GET** `localhost:8080/shows/get-all`

---

#### 3. Get Show by ID
**GET** `localhost:8080/shows/get/{showId}`

**Response:** `200 OK` (with Redis caching)

---

#### 4. Get Shows by Movie
**GET** `localhost:8080/shows/get-by-movie/{movieId}`

---

#### 5. Get Shows by Theater
**GET** `localhost:8080/shows/get-by-theater/{theaterId}`

---

#### 6. Update Show
**PUT** `localhost:8080/shows/update/{showId}`

**Request Body:** Same as Add Show

---

#### 7. Delete Show
**DELETE** `localhost:8080/shows/delete/{showId}`

---

### Ticket APIs

#### 1. Book Ticket
**POST** `localhost:8080/tickets/book`

**Headers:** `Authorization: Bearer <token>` (Authenticated)

**Request Body:**
```json
{
  "showId": 1,
  "userId": 1,
  "requestedSeats": ["S1-1A", "S1-1B", "S1-1C"]
}
```

**Note:** Seat numbers include screen prefix (e.g., "S1-1A" for Screen 1, Seat 1A)

**Response:** `201 CREATED`
```json
"Ticket booking request submitted successfully. Confirmation email will be sent shortly."
```

**Features:**
- ✅ Requires authentication (JWT token)
- ✅ Distributed locking (Redisson) prevents double booking
- ✅ Seat availability validation
- ✅ Async processing via Kafka
- ✅ Email notification sent asynchronously
- ✅ Updates trending movies counter
- ✅ Payment integration (process payment first, then book ticket)

---

#### 2. Cancel Ticket
**DELETE** `localhost:8080/tickets/cancel-ticket`

**Headers:** `Authorization: Bearer <token>` (Authenticated)

**Request Body:**
```json
{
  "ticketId": 1
}
```

**Response:** `200 OK`
```json
"Ticket cancellation request submitted successfully. Confirmation email will be sent shortly."
```

**Features:**
- ✅ Requires authentication (JWT token)
- ✅ Async processing via Kafka
- ✅ Email notification sent asynchronously
- ✅ Updates trending movies counter (decreases)

---

#### 3. Get All Tickets
**GET** `localhost:8080/tickets/get-all`

---

#### 4. Get Ticket by ID
**GET** `localhost:8080/tickets/get/{ticketId}`

**Response:** `200 OK` (with Redis caching)

---

#### 5. Get Ticket by Ticket ID (UUID)
**GET** `localhost:8080/tickets/get-by-ticket-id/{ticketId}`

---

#### 6. Get Tickets by User
**GET** `localhost:8080/tickets/get-by-user/{userId}`

**Response:** `200 OK` (with Redis caching)

---

#### 7. Get Tickets by Show
**GET** `localhost:8080/tickets/get-by-show/{showId}`

**Response:** `200 OK` (with Redis caching)

### Screen APIs

#### 1. Add Screen
**POST** `localhost:8080/screens/add`

**Headers:** `Authorization: Bearer <token>` (Admin or Theater Owner)

**Request Body:**
```json
{
  "name": "Screen 1",
  "screenNumber": 1,
  "theaterId": 1,
  "classicSeatsCount": 50,
  "premiumSeatsCount": 30
}
```

**Response:** `201 CREATED`
```json
"Screen creation request submitted successfully"
```

---

#### 2. Get All Screens
**GET** `localhost:8080/screens/get-all`

**Response:** `200 OK`

---

#### 3. Get Screen by ID
**GET** `localhost:8080/screens/get/{screenId}`

**Response:** `200 OK` (with Redis caching)

---

#### 4. Get Screens by Theater
**GET** `localhost:8080/screens/get-by-theater/{theaterId}`

**Response:** `200 OK`

---

#### 5. Update Screen
**PUT** `localhost:8080/screens/update/{screenId}`

**Headers:** `Authorization: Bearer <token>` (Admin or Theater Owner)

---

#### 6. Delete Screen
**DELETE** `localhost:8080/screens/delete/{screenId}`

**Headers:** `Authorization: Bearer <token>` (Admin or Theater Owner)

**Note:** Cannot delete screen with scheduled shows.

---

### Payment APIs

#### 1. Process Payment
**POST** `localhost:8080/payments/process`

**Headers:** `Authorization: Bearer <token>` (Authenticated)

**Request Body:**
```json
{
  "userId": 1,
  "ticketId": null,
  "ticketEntryDTO": {
    "showId": 1,
    "userId": 1,
    "requestedSeats": ["S1-1A", "S1-1B"]
  },
  "amount": 500.00,
  "paymentMethod": "CARD",
  "cardNumber": "****1234",
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Important:** 
- `idempotencyKey` is required (generate UUID client-side)
- Use same key for retries to prevent duplicate charges
- Payment must succeed before ticket booking

**Response:** `200 OK`
```json
{
  "paymentId": "pay_abc123",
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "amount": 500.00,
  "message": "Payment request received",
  "ticketId": null
}
```

---

#### 2. Get Payment Status
**GET** `localhost:8080/payments/status/{paymentId}`

**Headers:** `Authorization: Bearer <token>` (Authenticated)

**Response:** `200 OK`
```json
{
  "paymentId": "pay_abc123",
  "status": "SUCCESS",
  "amount": 500.00,
  "ticketId": 123,
  "message": "Payment successful"
}
```

---

#### 3. Get Payment by Idempotency Key
**GET** `localhost:8080/payments/status-by-key/{idempotencyKey}`

**Headers:** `Authorization: Bearer <token>` (Authenticated)

**Use Case:** Retrieve payment result after retry/network timeout.

---

## Payment Flow

### Overview

Payment processing includes:
- **Idempotency Key:** Prevents duplicate charges on retries
- **Payload Hash Verification:** Ensures retry requests have identical payload
- **Dummy Payment Gateway:** Simulated payment (90% success, fails for amounts > 10000)

### Payment Flow Steps

1. **Client generates idempotency key** (UUID)
2. **Client calls `/payments/process`** with booking details + idempotency key
3. **System checks idempotency** (Redis cache + Database)
4. **If new payment:** Creates PENDING payment → Processes via Kafka
5. **If retry:** Returns existing payment result (no duplicate charge)
6. **Payment processing** (simulates payment gateway)
7. **If successful:** Automatically triggers ticket booking
8. **Client polls payment status** to get result

### Example: Complete Payment + Booking Flow

```bash
# Step 1: Generate idempotency key (client-side)
IDEMPOTENCY_KEY=$(uuidgen)  # or use any unique identifier

# Step 2: Process payment
curl -X POST http://localhost:8080/payments/process \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "ticketEntryDTO": {
      "showId": 1,
      "userId": 1,
      "requestedSeats": ["S1-1A", "S1-1B"]
    },
    "amount": 500.00,
    "paymentMethod": "CARD",
    "idempotencyKey": "'$IDEMPOTENCY_KEY'"
  }'

# Response: {"paymentId": "pay_123", "status": "PENDING", ...}

# Step 3: Wait a moment (payment processing happens asynchronously)
sleep 2

# Step 4: Check payment status
curl -X GET http://localhost:8080/payments/status/pay_123 \
  -H "Authorization: Bearer <your-token>"

# Response: {"paymentId": "pay_123", "status": "SUCCESS", "ticketId": 456, ...}

# If payment fails, retry with SAME idempotency key:
curl -X POST http://localhost:8080/payments/process \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "ticketEntryDTO": {
      "showId": 1,
      "userId": 1,
      "requestedSeats": ["S1-1A", "S1-1B"]
    },
    "amount": 500.00,
    "paymentMethod": "CARD",
    "idempotencyKey": "'$IDEMPOTENCY_KEY'"  // SAME KEY!
  }'

# Response: Returns existing payment result (no duplicate charge)
```

---

## Kafka Integration

### Overview

Kafka is used for:
1. **Asynchronous Data Persistence** - All create/update/delete operations go through Kafka
2. **Event-Driven Architecture** - Services communicate via events
3. **Email Notifications** - Async email delivery
4. **Analytics** - Track bookings, cancellations, and user registrations

### Kafka Topics

| Topic | Purpose | Consumer Group |
|-------|---------|----------------|
| `user-creation-events` | User creation | `user-creation-group` |
| `user-update-events` | User updates | `user-update-group` |
| `user-deletion-events` | User deletions | `user-deletion-group` |
| `user-registration-events` | Analytics for new users | `user-registration-group` |
| `movie-creation-events` | Movie creation | `movie-creation-group` |
| `movie-update-events` | Movie updates | `movie-update-group` |
| `movie-deletion-events` | Movie deletions | `movie-deletion-group` |
| `theater-creation-events` | Theater creation | `theater-creation-group` |
| `theater-update-events` | Theater updates | `theater-update-group` |
| `theater-deletion-events` | Theater deletions | `theater-deletion-group` |
| `show-creation-events` | Show creation | `show-creation-group` |
| `show-update-events` | Show updates | `show-update-group` |
| `show-deletion-events` | Show deletions | `show-deletion-group` |
| `ticket-booking-requests` | Ticket booking requests | `ticket-booking-group` |
| `ticket-cancellation-requests` | Ticket cancellation requests | `ticket-cancellation-group` |
| `ticket-booking-events` | Analytics for bookings | `ticket-booking-events-group` |
| `ticket-cancellation-events` | Analytics for cancellations | `ticket-cancellation-events-group` |
| `email-notification` | Email notifications | `email-notification-group` |
| `payment-requests` | Payment processing requests | `payment-processing-group` |
| `payment-result-events` | Payment result events | `payment-events-group` |
| `screen-creation-events` | Screen creation | `screen-creation-group` |
| `screen-update-events` | Screen updates | `screen-update-group` |
| `screen-deletion-events` | Screen deletions | `screen-deletion-group` |

### Data Flow

```
API Request → Service Layer → Kafka Producer → Kafka Topic
                                                      ↓
                                          Kafka Consumer → Database
                                                      ↓
                                          Email Service / Analytics
```

For detailed Kafka documentation, see [KAFKA_IMPLEMENTATION_GUIDE.md](./KAFKA_IMPLEMENTATION_GUIDE.md)

---

## Redis Integration

### Use Cases

1. **Caching**
   - Movie by ID
   - Theater by ID
   - Show by ID
   - Ticket by ID
   - Tickets by User
   - Tickets by Show
   - Cache TTL: 24 hours

2. **Distributed Locking (Redisson)**
   - Seat booking locks (prevents double booking)
   - Lock key format: `seat-lock-{showId}-{seatNumber}`
   - Lock timeout: 5-10 seconds

3. **Trending Movies**
   - Redis Sorted Set (ZSet) tracks booking counts
   - Key: `trending::movie::count`
   - Automatically updated on ticket booking/cancellation

### Redis Keys

| Key Pattern | Type | Purpose |
|-------------|------|---------|
| `movie::{movieId}` | String | Movie cache |
| `theater::{theaterId}` | String | Theater cache |
| `show::{showId}` | String | Show cache |
| `ticket::{ticketId}` | String | Ticket cache |
| `user-tickets::{userId}` | String | User tickets cache |
| `show-tickets::{showId}` | String | Show tickets cache |
| `seat-lock-{showId}-{seatNumber}` | Lock | Seat booking lock |
| `movie-{movieName}` | String | Movie booking counter |
| `trending::movie::count` | ZSet | Trending movies sorted by count |

---

## Setup Instructions

### Prerequisites

1. **Java 17** or higher
2. **Maven 3.6+**
3. **MySQL** (or use Aiven Cloud MySQL)
4. **Redis** (or use Redis Cloud)
5. **Apache Kafka** (for event processing)

### Configuration

Update `application.yaml` with your credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-mysql-host:port/database
    username: your-username
    password: your-password
  
  data:
    redis:
      host: your-redis-host
      port: your-redis-port
      password: your-redis-password
  
  kafka:
    bootstrap-servers: localhost:9092
  
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password

# JWT Configuration
jwt:
  secret: YourSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS512AlgorithmToWorkProperlyAndSecurely
  expiration: 86400000 # 24 hours in milliseconds
```

### Running Kafka

1. **We are using Kafka 4.0, and it comes with kraft. No need to start zookeeper. Start kafka directly:**
```bash
bin/kafka-server-start.sh config/server.properties
```

2. **Verify Kafka is running:**
```bash
kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### Running the Application

1. **Clone the repository:**
```bash
git clone <repository-url>
cd Book_My_Show
```

2. **Build the project:**
```bash
mvn clean install
```

3. **Run the application:**
```bash
mvn spring-boot:run
```

4. **Access Swagger UI:**
```
http://localhost:8080/swagger-ui.html
```

### Testing

Use Postman or curl to test APIs. Example:

```bash
# Add a user
curl -X POST http://localhost:8080/user/add \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "age": 25
  }'
```

---

## Future Scope

1. ✅ **Multiple user handling** - Implemented
2. ✅ **Seat locking during payment** - Implemented with Redisson
3. ✅ **Multiple Screen handling in theater** - Implemented
4. ✅ **Payment Flow** - Implemented
5. ✅ **Login and User Account Management** - Implemented
6. ✅ **Authentication and Authorization** - Implemented
7. ⏳ **Payment Gateway Integration** - Pending
8. ⏳ **Real-time seat availability** - Pending
9. ⏳ **Movie recommendations** - Pending
10. ⏳ **Rating and reviews** - Pending
11. ⏳ **Discounts and coupons** - Pending
12. ⏳ **Mobile app** - Pending

---

## Performance Optimizations

The following optimizations have been implemented:

1. **Stream API** - All list operations use Java Streams for better performance
2. **Set-based lookups** - O(1) lookup complexity using HashSet
3. **String.join()** - Efficient string concatenation
4. **Enum comparisons** - Direct enum comparison instead of string comparison
5. **Redis caching** - Frequently accessed data cached for 24 hours
6. **Distributed locking** - Prevents race conditions in seat booking
7. **Kafka async processing** - Non-blocking API responses
8. **Transaction management** - `@Transactional` for data consistency
9. **Optional handling** - Proper null safety checks

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

---

## License

This project is for educational purposes.

---

## Contact

For issues or questions, please open an issue on GitHub.
