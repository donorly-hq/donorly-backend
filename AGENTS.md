# Donorly Backend

Spring Boot 4 (Java 17) REST API for nonprofit donor/fundraising management. Layered Spring MVC (Controller → Service → JPA Repository → PostgreSQL) with stateless JWT auth. Built with Maven (`./mvnw`). No frontend lives in this repo.

## Cursor Cloud specific instructions

### Stack / gotchas
- `CHANGES_README.md` is **stale**: it describes MongoDB Atlas, but the code was migrated to **PostgreSQL + Spring Data JPA**. Trust the code (`pom.xml`, `application.properties`), not that doc.
- Hibernate `ddl-auto=update` auto-creates/updates tables on startup, so no migrations are needed.
- The base VM does not ship PostgreSQL. It is installed during environment setup (PostgreSQL 16). If it is missing on a fresh VM, install with `sudo apt-get install -y postgresql postgresql-contrib`.

### Required env vars (app will not start without these)
The app reads a PostgreSQL datasource and JWT secret purely from env vars:
```
export PGHOST=127.0.0.1 PGPORT=5432 PGDATABASE=donorly PGUSER=donorly PGPASSWORD=donorly
export JWT_SECRET="$(openssl rand -base64 48)"
```
- `JWT_SECRET` **must be Base64 that decodes to ≥ 32 bytes** (HMAC-SHA256). `JwtUtil` base64-decodes it at construction, so a short/non-base64 value crashes startup.

### Start PostgreSQL + seed a local DB (one-time per fresh VM)
```
sudo pg_ctlcluster 16 main start
sudo -u postgres psql -c "CREATE ROLE donorly WITH LOGIN PASSWORD 'donorly';"
sudo -u postgres psql -c "CREATE DATABASE donorly OWNER donorly;"
```

### Run / test / build
- Tests: `./mvnw test` — the sole test is `@SpringBootTest contextLoads`, which **needs a reachable PostgreSQL + `JWT_SECRET`** (it boots the full context).
- Run (dev): `./mvnw spring-boot:run` → listens on port **8080** (DevTools auto-restart is on).
- Package: `./mvnw -DskipTests package`.

### Auth for manual testing (no public signup/setup endpoint)
Every `/api/**` route requires a JWT except `/api/auth/{admin,ambassador}/login` and `/api/auth/logout` (and `/api/setup/**`, which has no controller). There is no way to create the first admin over HTTP, so seed one directly in the DB with a Spring-compatible BCrypt hash. Generate the hash with Spring's own encoder (guarantees `$2a$` compatibility):
```
./mvnw -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
printf 'System.out.println("HASH:"+new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Admin123!"));\n/exit\n' \
  | jshell --class-path "$(cat /tmp/cp.txt)" 2>/dev/null | grep -o 'HASH:[^ ]*' | sed 's/HASH://'
```
Then insert: `INSERT INTO users (id,email,password,role,active) VALUES (gen_random_uuid()::text,'admin@donorly.org','<hash>','ADMIN',true);`

A seeded admin already exists in the local dev DB: `admin@donorly.org` / `Admin123!`.
Login: `POST /api/auth/admin/login {"email","password"}` → `{token}`, then send `Authorization: Bearer <token>` to other endpoints.
