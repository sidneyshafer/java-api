# Java API - Spring Boot REST API

A scalable, modular Spring Boot REST API with multi-database support, external SQL file execution, and comprehensive CRUD operations.

## Features

- **Layered Architecture**: Strict separation of concerns with controller, service, repository layers
- **Multi-Database Support**: Configure multiple datasources with separate connection pools
- **External SQL Files**: All SQL queries stored in `/sql` directory, no inline SQL
- **Pagination**: Built-in pagination support for all list endpoints
- **Optimistic Locking**: Version-based concurrency control
- **Soft Delete**: Resources are soft-deleted rather than permanently removed
- **Async Processing**: Support for asynchronous external API calls
- **Caching**: Redis-ready caching layer
- **API Versioning**: All endpoints under `/api/v1`
- **OpenAPI Documentation**: Swagger UI available at `/api/v1/swagger-ui.html`

## Project Structure

```
src/main/java/com/api/
├── Application.java              # Main entry point
├── config/                       # Configuration classes
│   ├── AsyncConfig.java         # Async execution config
│   ├── CacheConfig.java         # Caching configuration
│   ├── OpenApiConfig.java       # Swagger/OpenAPI config
│   ├── WebConfig.java           # CORS and web config
│   └── datasource/              # Dynamic datasource support
│       ├── DataSourceProperties.java   # Config properties binding
│       ├── DataSourceRegistry.java     # Named DB connection registry
│       └── DynamicDataSourceConfig.java # Bean configuration
├── controller/                   # REST controllers
│   ├── UserController.java
│   ├── ProductController.java
│   └── OrderController.java
├── dto/                          # Data Transfer Objects
│   ├── common/                   # Shared DTOs
│   │   ├── ApiResponse.java
│   │   ├── ErrorResponse.java
│   │   ├── PageRequest.java
│   │   └── PageResponse.java
│   ├── user/                     # User-specific DTOs
│   ├── product/                  # Product-specific DTOs
│   └── order/                    # Order-specific DTOs
├── exception/                    # Exception handling
│   ├── BusinessException.java
│   ├── ConflictException.java
│   ├── GlobalExceptionHandler.java
│   ├── OptimisticLockException.java
│   └── ResourceNotFoundException.java
├── model/                        # Domain models
│   ├── BaseEntity.java
│   ├── User.java
│   ├── Product.java
│   ├── Order.java
│   └── OrderItem.java
├── repository/                   # Data access layer
│   ├── BaseRepository.java      # Base with dynamic DB support
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── OrderRepository.java
│   └── InventoryRepository.java # Example: specific DB connection
├── service/
│   ├── internal/                 # Internal business logic
│   │   ├── UserService.java
│   │   ├── ProductService.java
│   │   └── OrderService.java
│   └── external/                 # External API integrations
│       ├── PaymentService.java
│       └── NotificationService.java
└── util/                         # Utility classes
    ├── PaginationHelper.java
    └── SqlLoader.java

src/main/resources/
├── application.yml               # Application configuration
├── schema.sql                    # Database schema
└── sql/                          # External SQL files

.env.example                      # Environment variables template
    ├── user/
    │   ├── create.sql
    │   ├── findById.sql
    │   ├── findAll.sql
    │   ├── update.sql
    │   ├── delete.sql
    │   └── ...
    ├── product/
    │   └── ...
    └── order/
        └── ...
```

## Requirements

- Java 17+
- Maven 3.6+
- Database (H2/PostgreSQL/MySQL)

## Quick Start

### 1. Clone and Build

```bash
cd java-api
mvn clean install
```

### 2. Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Or using the JAR
java -jar target/java-api-1.0.0.jar
```

### 3. Access the API

- **API Base URL**: `http://localhost:8080/api/v1`
- **Swagger UI**: `http://localhost:8080/api/v1/swagger-ui.html`
- **API Docs**: `http://localhost:8080/api/v1/api-docs`
- **H2 Console** (dev): `http://localhost:8080/api/v1/h2-console`

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure for your environment.

| Variable | Description | Default |
|----------|-------------|---------|
| `DEFAULT_DB_URL` | Default/primary database URL | `jdbc:h2:mem:defaultdb` |
| `DEFAULT_DB_USERNAME` | Default DB username | `sa` |
| `DEFAULT_DB_PASSWORD` | Default DB password | (empty) |
| `DEFAULT_DB_DRIVER` | Default DB driver | `org.h2.Driver` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `SERVER_PORT` | Server port | `8080` |
| `CACHE_TYPE` | Cache type (simple/redis) | `simple` |
| `ASYNC_CORE_POOL_SIZE` | Async thread pool core size | `5` |
| `DEFAULT_PAGE_SIZE` | Default pagination size | `20` |

### Multi-Database Setup

The API supports **unlimited named database connections**. Add databases under `app.datasources` in `application.yml`:

```yaml
app:
  datasources:
    # Primary database (set primary: true)
    DefaultDB:
      url: jdbc:postgresql://localhost:5432/myapp
      username: postgres
      password: secret
      driver-class-name: org.postgresql.Driver
      primary: true
      hikari:
        pool-name: DefaultDBPool
        maximum-pool-size: 10

    # Add more databases with any name
    InventoryDB:
      url: jdbc:postgresql://inventory-host:5432/inventory
      username: postgres
      password: secret
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 15

    OrdersDB:
      url: jdbc:mysql://orders-host:3306/orders
      username: root
      password: secret
      driver-class-name: com.mysql.cj.jdbc.Driver

    ReportingDB:
      url: jdbc:postgresql://reporting-host:5432/reports
      username: readonly
      password: secret
      driver-class-name: org.postgresql.Driver
```

### Using Named Databases

**In Repositories:**
```java
// Connect to a specific database by name
public class InventoryRepository extends BaseRepository<Product, Long> {
    public InventoryRepository(DataSourceRegistry registry, SqlLoader sqlLoader, PaginationHelper helper) {
        super(registry, "InventoryDB", sqlLoader, helper, "product");
    }
}
```

**Via DataSourceRegistry:**
```java
@Autowired
private DataSourceRegistry dataSourceRegistry;

// Get JdbcTemplate for any named database
NamedParameterJdbcTemplate inventoryJdbc = dataSourceRegistry.getNamedParameterJdbcTemplate("InventoryDB");
NamedParameterJdbcTemplate ordersJdbc = dataSourceRegistry.getNamedParameterJdbcTemplate("OrdersDB");

// List all available databases
Set<String> dbNames = dataSourceRegistry.getDataSourceNames(); // [DefaultDB, InventoryDB, OrdersDB, ...]

// Add a database at runtime
dataSourceRegistry.addDataSource("NewDB", "jdbc:postgresql://...", "user", "pass", "org.postgresql.Driver");

// Check if database exists
boolean exists = dataSourceRegistry.hasDataSource("InventoryDB");
```

## API Endpoints

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Create a new user |
| GET | `/users/{id}` | Get user by ID |
| GET | `/users/email/{email}` | Get user by email |
| GET | `/users` | Get all users (paginated) |
| GET | `/users/status/{status}` | Get users by status |
| GET | `/users/search?q={term}` | Search users by name |
| PUT | `/users/{id}` | Update user |
| DELETE | `/users/{id}` | Delete user (soft) |

### Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/products` | Create a new product |
| GET | `/products/{id}` | Get product by ID |
| GET | `/products/sku/{sku}` | Get product by SKU |
| GET | `/products` | Get all products (paginated) |
| GET | `/products/category/{category}` | Get products by category |
| GET | `/products/search?q={term}` | Search products by name |
| PUT | `/products/{id}` | Update product |
| PATCH | `/products/{id}/quantity?change={n}` | Update quantity |
| DELETE | `/products/{id}` | Delete product (soft) |

### Orders

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/orders` | Create a new order |
| GET | `/orders/{id}` | Get order by ID |
| GET | `/orders/number/{orderNumber}` | Get order by number |
| GET | `/orders` | Get all orders (paginated) |
| GET | `/orders/user/{userId}` | Get orders by user |
| GET | `/orders/status/{status}` | Get orders by status |
| PUT | `/orders/{id}` | Update order |
| PATCH | `/orders/{id}/status?status={status}` | Update status |
| DELETE | `/orders/{id}` | Cancel order |

## Request/Response Examples

### Create User

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1234567890",
    "role": "USER"
  }'
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "User created successfully",
  "data": {
    "id": 1,
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "phone": "+1234567890",
    "status": "ACTIVE",
    "role": "USER",
    "createdAt": "2026-02-15T10:30:00",
    "version": 1
  },
  "timestamp": "2026-02-15T10:30:00"
}
```

### Paginated List

**Request:**
```bash
curl "http://localhost:8080/api/v1/products?page=0&size=10&sortBy=name&sortDirection=ASC"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  },
  "timestamp": "2026-02-15T10:30:00"
}
```

### Error Response

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with ID: 999",
  "path": "/api/v1/users/999",
  "timestamp": "2026-02-15T10:30:00"
}
```

## Scalability Features

### Stateless Design
- No server-side session state
- Horizontally scalable

### Connection Pooling
- HikariCP with configurable pool sizes per database
- Separate connection pools for each named database
- Runtime database registration support

### Caching
- In-memory caching for single instance
- Redis-ready for distributed caching

### Async Processing
- Configurable thread pools for async operations
- Dedicated executor for external API calls

### Pagination
- Configurable default and max page sizes
- Index-aware queries with proper LIMIT/OFFSET

## External SQL Files

SQL queries are stored in `/src/main/resources/sql/{module}/{operation}.sql`:

```sql
-- /sql/user/findById.sql
SELECT 
    id, email, first_name, last_name, phone,
    status, role, created_at, updated_at, deleted, version
FROM users
WHERE id = :id AND deleted = false
```

The `SqlLoader` utility loads and caches these queries at startup.

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## Production Deployment

1. Copy `.env.example` to `.env` and configure production values
2. Configure all required databases under `app.datasources`
3. Set appropriate pool sizes for expected load
4. Enable Redis for distributed caching if running multiple instances
5. Review and adjust logging levels
6. Enable actuator endpoints for monitoring

**Load environment and run:**
```powershell
# PowerShell - Load .env and run
Get-Content .env | ForEach-Object { 
  if ($_ -match '^([^#].+?)=(.*)$') { 
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2]) 
  } 
}
java -jar target/java-api-1.0.0.jar
```

```bash
# Bash - Load .env and run
export $(grep -v '^#' .env | xargs)
java -jar target/java-api-1.0.0.jar
```

**Or pass variables directly:**
```bash
java -jar java-api-1.0.0.jar \
  --spring.profiles.active=prod
```

## License

MIT License
