
# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W — Client-Server Architectures 
***Student Name** : Ganeshamoorthy Ackaash

**Technology Stack:** JAX-RS (Jersey 2.40) + Apache Tomcat 9 + Jackson JSON

---

## Table of Contents

1. [API Overview](#api-overview)
2. [Project Structure](#project-structure)
3. [Build & Run Instructions](#build--run-instructions)
4. [Sample curl Commands](#sample-curl-commands)
5. [Conceptual Report — Question Answers](#conceptual-report--question-answers)

---

## API Overview

A fully RESTful API for the University Smart Campus initiative, managing campus **Rooms** and **IoT Sensors**. Built exclusively with JAX-RS (Jersey 2.40) deployed on **Apache Tomcat 9**. All data is held in thread-safe `ConcurrentHashMap` in-memory structures — no database is used as per the coursework specification.

The application is packaged as a **WAR file** and deployed into Apache Tomcat. Tomcat acts as the servlet container — it receives all incoming HTTP requests and delegates them to Jersey, which handles all JAX-RS routing and processing.

### Base URL
```
http://localhost:8080/smart-campus-api/api/v1
```

### Resource Hierarchy

```
/api/v1/                             → Discovery (HATEOAS metadata)
/api/v1/rooms                        → Room collection (GET, POST)
/api/v1/rooms/{roomId}               → Single room (GET, DELETE)
/api/v1/sensors                      → Sensor collection (GET with ?type= filter, POST)
/api/v1/sensors/{sensorId}           → Single sensor (GET)
/api/v1/sensors/{sensorId}/readings  → Sub-resource: reading history (GET, POST)
```

### HTTP Status Codes Used

| Code | Meaning | When Used |
|------|---------|-----------|
| 200 | OK | Successful GET |
| 201 | Created | Successful POST — includes Location header |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Missing required fields in request body |
| 403 | Forbidden | POST reading to MAINTENANCE or OFFLINE sensor |
| 404 | Not Found | Resource ID does not exist |
| 409 | Conflict | DELETE room that still has sensors assigned |
| 415 | Unsupported Media Type | Wrong Content-Type sent to endpoint |
| 422 | Unprocessable Entity | Sensor POST with non-existent roomId |
| 500 | Internal Server Error | Any unexpected runtime exception |

### Seeded Data (available immediately on startup)

| Type | ID | Details |
|---|---|---|
| Room | LIB-301 | Library Quiet Study, capacity 50 |
| Room | LAB-101 | Computer Science Lab, capacity 30 |
| Sensor | TEMP-001 | Temperature, ACTIVE, assigned to LIB-301 |
| Sensor | CO2-002 | CO2, ACTIVE, assigned to LIB-301 |
| Sensor | OCC-003 | Occupancy, MAINTENANCE, assigned to LAB-101 |

---

## Project Structure

```
smartcampus-tomcat/
├── README.md
├── pom.xml                                          # Maven build — WAR packaging
└── src/
    └── main/
        ├── java/
        │   └── com/smartcampus/
        │       ├── SmartCampusApplication.java      # JAX-RS ResourceConfig
        │       ├── model/
        │       │   ├── Room.java                    # Room POJO
        │       │   ├── Sensor.java                  # Sensor POJO
        │       │   ├── SensorReading.java            # SensorReading POJO
        │       │   └── ErrorResponse.java            # Standard error body
        │       ├── store/
        │       │   └── DataStore.java               # Singleton in-memory store
        │       ├── resource/
        │       │   ├── DiscoveryResource.java        # GET /api/v1/
        │       │   ├── RoomResource.java             # /api/v1/rooms
        │       │   ├── SensorResource.java           # /api/v1/sensors
        │       │   └── SensorReadingResource.java    # Sub-resource /readings
        │       ├── exception/
        │       │   ├── RoomNotEmptyException.java
        │       │   ├── LinkedResourceNotFoundException.java
        │       │   ├── SensorUnavailableException.java
        │       │   └── mapper/
        │       │       ├── RoomNotEmptyExceptionMapper.java       # 409
        │       │       ├── LinkedResourceNotFoundExceptionMapper.java # 422
        │       │       ├── SensorUnavailableExceptionMapper.java  # 403
        │       │       └── GlobalExceptionMapper.java             # 500
        │       └── filter/
        │           └── LoggingFilter.java            # Request + Response logger
        └── webapp/
            └── WEB-INF/
                └── web.xml                          # Tomcat servlet configuration
```

---

## Build & Run Instructions

### Prerequisites

- Java 11 or higher
- Apache Maven 3.6+
- Apache Tomcat 9 (download from tomcat.apache.org)
- NetBeans IDE 12+ (recommended)

---

### Option 1 — Run via NetBeans (Recommended)

**Step 1 — Open the project in NetBeans**
```
File → Open Project → select the smartcampus-tomcat folder
```
NetBeans recognises it as a Maven project automatically via `pom.xml`.

**Step 2 — Register Tomcat in NetBeans (first time only)**
```
Tools → Servers → Add Server → Apache Tomcat or TomEE
→ Browse to your Tomcat folder (e.g. C:\tomcat9\apache-tomcat-9.0.x)
→ Set Username: admin
→ Set Password: admin
→ Leave "Create user if it does not exist" ticked
→ Click Finish
```

**Step 3 — Run the project**
```
Right-click project → Run
```
NetBeans builds the WAR file, deploys it to Tomcat, and starts the server automatically.

**Step 4 — Verify the server is running**

Open Postman and send:
```
GET http://localhost:8080/smart-campus-api/api/v1/
```
Expected: `200 OK` with JSON discovery metadata.

---

### Option 2 — Build manually and deploy to Tomcat

**Step 1 — Build the WAR file**
```bash
cd smartcampus-tomcat
mvn clean package
```
This produces `target/smart-campus-api.war`

**Step 2 — Copy the WAR into Tomcat's webapps folder**
```bash
# Windows
copy target\smart-campus-api.war C:\tomcat9\apache-tomcat-9.0.x\webapps\



**Step 3 — Start Tomcat**
```bash
# Windows
C:\tomcat9\apache-tomcat-9.0.x\bin\startup.bat

# Mac / Linux
/opt/tomcat/bin/startup.sh
```

**Step 4 — Verify**
```
GET http://localhost:8080/smart-campus-api/api/v1/
```

**Step 5 — Stop Tomcat when done**
```bash
# Windows
C:\tomcat9\apache-tomcat-9.0.x\bin\shutdown.bat



---

### How Tomcat + JAX-RS Work Together

When you run the project:

1. **Tomcat** starts and reads `WEB-INF/web.xml`
2. `web.xml` tells Tomcat to load **Jersey's ServletContainer** as the servlet
3. Jersey reads **SmartCampusApplication.java** and scans all `com.smartcampus` packages
4. All `@Path`, `@Provider`, and filter classes are registered automatically
5. Every incoming HTTP request is handled by Tomcat → passed to Jersey → routed to the correct resource class

---

## Sample curl Commands

### 1. Discovery — GET /api/v1/
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/ \
  -H "Accept: application/json"
```
**Expected:** `200 OK` with API name, version, contact, and `_links` map.

---

### 2. List all rooms — GET /api/v1/rooms
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Accept: application/json"
```
**Expected:** `200 OK` with array containing LIB-301 and LAB-101.

---

### 3. Create a room — POST /api/v1/rooms
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"ENG-204\",\"name\":\"Engineering Lab\",\"capacity\":40}"
```
**Expected:** `201 Created` with the new room and a `Location` header pointing to `/api/v1/rooms/ENG-204`.

---

### 4. Create a sensor in that room — POST /api/v1/sensors
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"TEMP-099\",\"type\":\"Temperature\",\"status\":\"ACTIVE\",\"currentValue\":22.1,\"roomId\":\"ENG-204\"}"
```
**Expected:** `201 Created`. Room ENG-204 now lists TEMP-099 in its `sensorIds`.

---

### 5. Verify sensor is linked to the room — GET /api/v1/rooms/ENG-204
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms/ENG-204 \
  -H "Accept: application/json"
```
**Expected:** `200 OK` — `sensorIds` array contains `TEMP-099`.

---

### 6. Filter sensors by type — GET /api/v1/sensors?type=CO2
```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```
**Expected:** `200 OK` — only CO2 sensors returned. Omit `?type=` to get all sensors.

---

### 7. Post a sensor reading — POST /api/v1/sensors/{sensorId}/readings
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-099/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":28.5}"
```
**Expected:** `201 Created`. Sensor TEMP-099 `currentValue` automatically updates to `28.5`.

---

### 8. Get reading history — GET /api/v1/sensors/{sensorId}/readings
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-099/readings \
  -H "Accept: application/json"
```
**Expected:** `200 OK` with array of all readings for TEMP-099.

---

### 9. Delete room with sensors — triggers 409 Conflict
```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301
```
**Expected:** `409 Conflict` — JSON error explaining room still has sensors assigned.

---

### 10. Sensor with invalid roomId — triggers 422 Unprocessable Entity
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0,\"roomId\":\"FAKE-999\"}"
```
**Expected:** `422 Unprocessable Entity` — roomId does not exist.

---

### 11. Reading on MAINTENANCE sensor — triggers 403 Forbidden
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/OCC-003/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":5}"
```
**Expected:** `403 Forbidden` — OCC-003 is in MAINTENANCE status.

---

## Conceptual Report — Question Answers

---

### Part 1.1 — JAX-RS Resource Lifecycle

**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronise your in-memory data structures.

By default, JAX-RS creates a **new instance of every resource class for each incoming HTTP request** (Request-Scoped lifecycle). This means any instance-level fields initialised in a resource constructor are destroyed the moment the request completes — they cannot act as shared state across requests.

This has a direct impact on in-memory data management. If data were stored as instance fields (e.g., `private Map<String, Room> rooms = new HashMap<>()`), every request would start with a completely empty map, making it impossible to persist any data between calls.

To solve this, the project uses a **Singleton `DataStore`** class — instantiated exactly once by the JVM class loader using the eager singleton pattern. All resource classes call `DataStore.getInstance()` to access the shared maps. Because the same instance is always returned regardless of which resource instance is calling, all request-scoped resources always operate on the same underlying dataset.

Thread safety is achieved by using **`ConcurrentHashMap`** instead of `HashMap`. When multiple clients send requests simultaneously on different Tomcat threads, they all operate on the same map instance. `ConcurrentHashMap` partitions its internal structure into segments, allowing concurrent reads without locking and serialising only conflicting writes — preventing race conditions and data corruption without requiring explicit `synchronized` blocks.

---

### Part 1.2 — HATEOAS

**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

HATEOAS (Hypermedia As The Engine Of Application State) is the principle that API responses should embed navigational links, enabling clients to discover available actions dynamically rather than relying on out-of-date static documentation.

**Benefits for client developers:**

- **Self-documenting API:** A client hitting `GET /api/v1/` receives a complete map of all available resource URLs without needing prior knowledge of the URL structure.
- **Decoupled from URL structure:** If the server restructures paths (e.g. v1 to v2), clients following links from responses adapt automatically without code changes.
- **Reduced hardcoding:** Clients that follow links from responses are resilient to server-side changes, whereas clients with hardcoded URLs break immediately.
- **Discoverability:** New endpoints added to the API appear in the discovery response immediately — clients can use them without consulting updated documentation.

Without HATEOAS, client developers must maintain their own copy of the API's URL structure, which becomes stale every time the server changes. HATEOAS makes the server the single source of truth for navigation.

---

### Part 2.1 — IDs vs Full Objects in List Responses

**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.

Returning **IDs only** produces a minimal payload and reduces bandwidth, but forces the client to make N additional GET requests to retrieve details for each room — the N+1 request problem. This suits scenarios where the client only needs a count or a list of names, and most items will never be inspected further.

Returning **full objects** requires a single round-trip and gives the client everything it needs immediately — suitable when a complete table of room names, capacities, and sensor assignments must be displayed at once. The trade-off is a larger payload, which may be significant when there are hundreds of rooms.

This API returns full objects by default, as campus facilities managers typically need to see room names, capacities, and assigned sensors at a glance without making additional per-room requests.

---

### Part 2.2 — DELETE Idempotency

**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.

Strictly speaking, the implementation is **not idempotent in response code**, but it is **idempotent in server state** — which is the correct REST definition.

- **First DELETE** on room `ENG-204`: room exists → deleted → `204 No Content`
- **Second DELETE** on room `ENG-204`: room no longer exists → `404 Not Found`

The response code differs, but the **server state** after both calls is identical: the room does not exist. No data corruption occurs and no unexpected side effects are triggered.

REST's idempotency guarantee, as defined in RFC 7231, concerns the **effect on server state** — not the HTTP status code returned. The specification states that a method is idempotent if repeated identical requests have the same effect on the server. Therefore this implementation is correct REST behaviour.

---

### Part 3.1 — @Consumes and Media Type Mismatches

**Question:** Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml, to a `@Consumes(MediaType.APPLICATION_JSON)` endpoint. How does JAX-RS handle this mismatch?

JAX-RS inspects the `Content-Type` header of the incoming request and compares it against the `@Consumes` annotation on the matching resource method. If they do not match, JAX-RS rejects the request **before the resource method is ever invoked** and automatically returns **HTTP 415 Unsupported Media Type**.

No custom error handling code is required — this enforcement is built into the JAX-RS runtime. The client receives a clear signal that the format of their request body is incompatible with what the server accepts. This enforces a strict media type contract: the API exclusively speaks JSON, and any deviation is cleanly rejected at the framework layer before any business logic runs.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

**Question:** You implemented filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?

Path parameters identify **specific, discrete resources** (e.g. `/sensors/TEMP-001`). Query parameters **modify or filter a collection operation**. Using a path segment for filtering violates REST semantics — it implies `/sensors/type/CO2` is itself a distinct resource, not a filtered view of the sensors collection.

| Consideration | `?type=CO2` (QueryParam) | `/type/CO2` (PathParam) |
|---|---|---|
| Optional | Yes — omitting returns all sensors | No — path always includes `/type/` |
| Multiple filters | `?type=CO2&status=ACTIVE` naturally | `/type/CO2/status/ACTIVE` — unreadable |
| REST convention | Correct — filters a collection | Wrong — implies a distinct sub-resource |
| Caching | CDNs correctly treat as a parameterised view | May be cached as a distinct resource |

Query parameters are optional by nature, compose naturally for multi-criteria filtering, and correctly signal to both clients and infrastructure that this is a filtered view of an existing collection rather than a new resource identifier.

---

### Part 4.1 — Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs?

Without sub-resource locators, all nested paths must be defined in one resource class. As the API grows, this creates a monolithic controller violating the Single Responsibility Principle.

The Sub-Resource Locator pattern delegates handling of a URL subtree to a dedicated class. In this project, `SensorResource` handles `/sensors`. When a request arrives at `/sensors/{id}/readings`, `SensorResource` returns a `SensorReadingResource` instance — JAX-RS invokes the appropriate method on that instance. `SensorReadingResource` is entirely focused on reading history and has no knowledge of sensor registration or filtering.

**Key benefits:**
- **Separation of concerns:** each class has one responsibility
- **Independent testability:** `SensorReadingResource` can be tested in isolation
- **Open/Closed Principle:** new sub-resources added without modifying the parent
- **Scalability:** each level remains manageable as the API grows

---

### Part 5.2 — Why 422 Is More Accurate Than 404

**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

**404 Not Found** means the URL endpoint requested does not exist. In the case of `POST /api/v1/sensors` with a non-existent `roomId`:

- The URL `/api/v1/sensors` **exists and was found** — this is not a 404 situation
- The request body is syntactically valid JSON — this is not a 400 Bad Request
- The problem is a **broken reference inside the payload**: the `roomId` points to a room that does not exist

**422 Unprocessable Entity** (RFC 4918) was designed precisely for this — the server understands the format but cannot process the semantic content. It tells the client: "I received valid JSON and understood your intent, but the data inside is semantically invalid." A 404 would mislead the client into thinking the endpoint itself was missing.

---

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather?

A raw stack trace is an attacker's reconnaissance report. It exposes:

- **Framework and library versions** (e.g. `jersey-server-2.40.jar`) — enables targeted CVE exploitation
- **Internal package and class names** (e.g. `com.smartcampus.store.DataStore`) — reveals project architecture
- **Source file names and line numbers** — pinpoints where to look for vulnerabilities
- **Business logic clues** — method names reveal domain logic that can be bypassed
- **Server infrastructure details** — OS paths and JVM version help fingerprint the environment

The `GlobalExceptionMapper` ensures no `Throwable` ever reaches the client as a stack trace. All unhandled exceptions are logged server-side and the client receives only a generic `500 Internal Server Error` JSON body.

---

### Part 5.5 — Filters vs Manual Logging

**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?

Manual logging across 15+ methods leads to:
- **Code duplication** — 30+ identical log statements scattered across the codebase
- **Inconsistency** — different developers format entries differently
- **High maintenance cost** — changing the log format requires editing every method
- **Violation of SRP** — business logic interleaved with infrastructure concerns
- **Error-prone** — new endpoints added without logging go unobserved

A single `LoggingFilter` implementing both `ContainerRequestFilter` and `ContainerResponseFilter` intercepts every request and response automatically. Logging is guaranteed for all endpoints including future ones, with zero changes to resource classes. This is the standard industry approach to cross-cutting concerns — logging, authentication, CORS, and rate limiting all belong in filters, not business logic.

---

