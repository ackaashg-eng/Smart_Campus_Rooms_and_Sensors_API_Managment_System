# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W — Client-Server Architectures  
**Student:** [Your Name]  
**Technology Stack:** JAX-RS (Jersey 2.40) + Grizzly HTTP Server + Jackson JSON

---

## API Overview

A fully RESTful API managing campus **Rooms** and **Sensors** for the University Smart Campus initiative. Built exclusively with JAX-RS — no Spring, no databases. All data is held in thread-safe `ConcurrentHashMap` in-memory structures.

### Resource Hierarchy

```
/api/v1                          → Discovery (HATEOAS metadata)
/api/v1/rooms                    → Room collection
/api/v1/rooms/{roomId}           → Single room (GET, DELETE)
/api/v1/sensors                  → Sensor collection (GET with ?type= filter, POST)
/api/v1/sensors/{sensorId}       → Single sensor
/api/v1/sensors/{sensorId}/readings  → Sub-resource: reading history (GET, POST)
```

### Status Codes Used

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Successful GET |
| 201 | Created | Successful POST |
| 204 | No Content | Successful DELETE |
| 403 | Forbidden | POST reading to MAINTENANCE/OFFLINE sensor |
| 404 | Not Found | Resource ID does not exist |
| 409 | Conflict | DELETE room that still has sensors |
| 422 | Unprocessable Entity | Sensor POST with non-existent roomId |
| 500 | Internal Server Error | Any unexpected runtime exception |

---

## Build & Run Instructions

### Prerequisites
- Java 11 or higher
- Apache Maven 3.6+

### Step 1 — Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
```

### Step 2 — Build the fat JAR
```bash
mvn clean package
```
This produces `target/smart-campus-api-1.0.0.jar` — a self-contained executable JAR with Grizzly embedded.

### Step 3 — Run the server
```bash
java -jar target/smart-campus-api-1.0.0.jar
```

The server starts on **http://localhost:8080**.  
The API base is **http://localhost:8080/api/v1**.  
Press `ENTER` in the terminal to stop the server.

---

## Sample curl Commands

### 1. Discovery — GET /api/v1
```bash
curl -X GET http://localhost:8080/api/v1 \
  -H "Accept: application/json"
```
**Expected:** 200 OK with API metadata and `_links` map.

---

### 2. Create a Room — POST /api/v1/rooms
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"ENG-204","name":"Engineering Lab","capacity":40}'
```
**Expected:** 201 Created with `Location` header pointing to the new room.

---

### 3. Create a Sensor (valid roomId) — POST /api/v1/sensors
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-099","type":"Temperature","status":"ACTIVE","currentValue":22.1,"roomId":"ENG-204"}'
```
**Expected:** 201 Created.

---

### 4. Filter Sensors by Type — GET /api/v1/sensors?type=CO2
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```
**Expected:** 200 OK with only CO2 sensors in the list.

---

### 5. Post a Sensor Reading — POST /api/v1/sensors/{sensorId}/readings
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}'
```
**Expected:** 201 Created. Parent sensor's `currentValue` updated to `23.7`.

---

### 6. Delete a Room with Sensors (409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```
**Expected:** 409 Conflict with JSON error body explaining room has active sensors.

---

### 7. Register Sensor with Invalid roomId (422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type":"CO2","status":"ACTIVE","currentValue":0,"roomId":"DOES-NOT-EXIST"}'
```
**Expected:** 422 Unprocessable Entity.

---

### 8. Post Reading to MAINTENANCE Sensor (403)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-003/readings \
  -H "Content-Type: application/json" \
  -d '{"value":5}'
```
**Expected:** 403 Forbidden (OCC-003 is seeded as MAINTENANCE).

---

## Conceptual Report — Question Answers

---

### Part 1.1 — JAX-RS Resource Lifecycle

**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance created per request or is it a singleton? How does this affect in-memory data management?

By default, JAX-RS creates a **new instance of every resource class for each incoming HTTP request** (Request-Scoped lifecycle). This means any instance-level fields initialised in a resource constructor are lost the moment the request completes — they cannot act as shared state.

This has a direct impact on in-memory data management. If data were stored as instance fields (e.g., `private Map<String, Room> rooms = new HashMap<>()`), every request would start with an empty map. To solve this, this project uses a **Singleton `DataStore`** class — instantiated exactly once by the JVM class loader. All resource classes call `DataStore.getInstance()` to access the shared maps.

Thread safety is achieved by using `ConcurrentHashMap` instead of `HashMap`. When multiple clients send requests simultaneously, their threads all operate on the same map instance. `ConcurrentHashMap` partitions its internal structure into segments, allowing concurrent reads without locking and serialising only conflicting writes — preventing race conditions and data corruption without requiring `synchronized` blocks on every method.

---

### Part 1.2 — HATEOAS

**Question:** Why is Hypermedia (HATEOAS) considered a hallmark of advanced RESTful design? How does it benefit client developers?

HATEOAS (Hypermedia As The Engine Of Application State) is the principle that API responses should include navigational links, allowing clients to discover available actions dynamically rather than relying on out-of-date static documentation.

**Benefits for client developers:**

1. **Self-documenting API:** A client hitting `GET /api/v1` receives a map of all available resource URLs. They do not need to know these in advance — the API tells them.
2. **Decoupled from URL structure:** If the server changes `/api/v1/rooms` to `/api/v2/rooms`, clients following links from the discovery endpoint automatically adapt without code changes.
3. **Reduced hardcoding:** Clients that hardcode URLs break whenever the server restructures its paths. Clients that follow links from responses are resilient to such changes.
4. **Discoverability:** New endpoints added to the API appear in the discovery response immediately — clients can find and use them without consulting updated documentation.

---

### Part 2.1 — IDs vs Full Objects in List Responses

**Question:** When returning a list of rooms, what are the implications of returning only IDs vs full room objects?

| Approach | Pros | Cons |
|---|---|---|
| IDs only | Minimal payload, fast response, low bandwidth | Client must make N additional requests to fetch details |
| Full objects | One request returns everything the client needs | Large payload, higher bandwidth, slower if there are many rooms |

**ID-only** suits scenarios where the client only needs to display a count or a list of names — further detail is fetched on demand. This is the "lazy loading" pattern and is appropriate when rooms have many nested fields or when most IDs are never clicked.

**Full objects** suit scenarios where the client will display a table of all rooms with their details immediately. One round-trip instead of N+1 is a major performance improvement.

This API returns **full objects** by default, as campus facilities managers typically need to see room names and capacities at a glance without making additional requests.

---

### Part 2.2 — DELETE Idempotency

**Question:** Is DELETE idempotent in your implementation?

**Strictly: no. Practically: yes — and this is the accepted REST standard.**

- **First DELETE** on room `ENG-204`: room exists → deleted → `204 No Content`
- **Second DELETE** on room `ENG-204`: room does not exist → `404 Not Found`

The response *code* differs, but the **server state** after both calls is identical: the room does not exist. REST's idempotency guarantee concerns **state**, not response codes. The RFC 7231 specification explicitly states that idempotency means repeated identical requests have the same effect on the server — not that they return the same status code. Therefore, this implementation is correct REST behaviour.

---

### Part 3.1 — @Consumes and Media Type Mismatches

**Question:** What happens technically if a client sends `text/plain` or `application/xml` to a `@Consumes(APPLICATION_JSON)` endpoint?

JAX-RS inspects the `Content-Type` header of the incoming request and compares it against the `@Consumes` annotation on the matching resource method. If they do not match, JAX-RS rejects the request **before** the resource method is ever invoked and automatically returns **HTTP 415 Unsupported Media Type**.

No custom error handling code is required — this is built into the JAX-RS runtime. The client receives a clear signal that the format of their request body is incompatible with what the server accepts. This enforces a strict contract: the API exclusively speaks JSON, and any deviation is cleanly rejected at the framework layer.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

**Question:** Why is `GET /sensors?type=CO2` superior to `GET /sensors/type/CO2`?

| Design | `?type=CO2` (QueryParam) | `/type/CO2` (PathParam) |
|---|---|---|
| Semantics | Filters a collection | Suggests a distinct resource |
| Optional | Yes — omitting returns all sensors | No — the path always includes `/type/...` |
| Multiple filters | `?type=CO2&status=ACTIVE` naturally | `/type/CO2/status/ACTIVE` becomes unreadable |
| REST convention | Collections filtered by params | Sub-paths identify specific resources |
| Caching | Proper CDN cache keys | Ambiguous — cache may treat as new resource |

Path parameters identify **specific resources** (e.g., `/sensors/TEMP-001`). Query parameters **modify or filter** a collection operation. Using a path segment for filtering violates REST semantics — it implies `/sensors/type/CO2` is itself a distinct resource, not a filtered view of the sensors collection. Query parameters compose naturally for multi-criteria search without restructuring the URL hierarchy.

---

### Part 4.1 — Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern.

Without sub-resource locators, all paths — including `/sensors/{id}/readings/{rid}` — must be defined in one resource class. As the API grows, this leads to a monolithic controller handling dozens of unrelated concerns: sensor CRUD, reading history, filtering, state management — all in one file. This violates the Single Responsibility Principle and makes the code increasingly difficult to test, maintain, and extend.

The Sub-Resource Locator pattern delegates handling of a URL subtree to a dedicated class. In this project, `SensorResource` handles `/sensors` and its direct children. When a request arrives at `/sensors/{id}/readings`, `SensorResource` returns a `SensorReadingResource` instance — JAX-RS then invokes the appropriate method on that instance. `SensorReadingResource` is entirely focused on reading history and knows nothing about sensor registration or filtering.

**Benefits:**
1. **Separation of concerns:** Each class has one responsibility — smaller, more readable files.
2. **Independent testability:** `SensorReadingResource` can be unit-tested in isolation without bootstrapping the entire sensor subsystem.
3. **Reusability:** The sub-resource class could theoretically be reused under a different parent path.
4. **Scalability:** As the API grows, new sub-resources can be added without modifying the parent class.

---

### Part 5.2 — Why 422 Is More Accurate Than 404

**Question:** Why is HTTP 422 more semantically accurate than 404 when a JSON payload references a missing entity?

**404 Not Found** means the URL endpoint that was requested does not exist on the server. The client navigated to a location that doesn't exist.

In the case of `POST /api/v1/sensors` with a non-existent `roomId`:
- The URL `/api/v1/sensors` **exists and was found** — this is not a 404 situation.
- The request body is syntactically valid JSON — this is not a 400 Bad Request.
- The semantic problem is that the **payload contains a broken reference**: the `roomId` field points to a room that does not exist in the system.

**422 Unprocessable Entity** (RFC 4918) was designed precisely for this situation — the server understands the request format but cannot process the **semantic content** of the payload. It communicates: "I received valid JSON, I understood your intent, but the data inside makes it impossible to fulfil." This gives client developers far more actionable feedback than a generic 404.

---

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

**Question:** What specific information could an attacker gather from a raw Java stack trace?

A raw stack trace is an attacker's reconnaissance report. It exposes:

1. **Framework and library versions** (e.g., `jersey-server-2.40.jar`) — the attacker can look up known CVEs for that exact version and craft a targeted exploit.
2. **Internal package and class names** (e.g., `com.smartcampus.store.DataStore.getReadingsForSensor`) — reveals the project's internal architecture and naming conventions.
3. **Source file names and line numbers** (e.g., `DataStore.java:87`) — pinpoints exactly where to look for vulnerabilities in leaked or decompiled code.
4. **Business logic clues** — method names like `validateRoomOwnership` or `calculateBillingRate` reveal domain logic the attacker can attempt to bypass.
5. **Server infrastructure details** — OS paths, JVM version, and deployment structure that help fingerprint the server environment.

The `GlobalExceptionMapper` in this project ensures that **no Throwable ever reaches the client as a stack trace**. All unhandled exceptions are logged server-side (where only authorised engineers can see them) and the client receives only a generic, information-free `500 Internal Server Error` JSON body.

---

### Part 5.5 — Filters vs Manual Logging

**Question:** Why use JAX-RS filters for cross-cutting concerns like logging rather than manual Logger.info() calls?

**Manual logging** requires inserting `logger.info()` at the start and end of every resource method. With 15+ methods across 4 resource classes, this means:
- 30+ identical log statements spread across the codebase
- Inconsistent formats (each developer formats differently)
- High maintenance cost when the log format needs updating
- Business logic interleaved with infrastructure code (violates Single Responsibility Principle)
- Easy to forget — new methods added without logging go unobserved

**Filter-based logging** solves all of these:
- **One class, one place** — `LoggingFilter` handles all requests and responses
- **Guaranteed execution** — filters cannot be accidentally skipped when adding new endpoints
- **Uniform format** — every log line is structured identically
- **Zero coupling** — resource methods contain only business logic; the filter is invisible to them
- **Easy to extend** — adding request timing, authentication logging, or correlation IDs requires changing only the filter

This is the standard industry approach to cross-cutting concerns (logging, authentication, CORS, rate limiting) — it is the reason JAX-RS filters exist.

---

*Report prepared as part of 5COSC022W Coursework — Smart Campus API*
