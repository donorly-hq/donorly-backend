# Donorly Backend — Phase 3 Changes Applied

This is your actual `donorly-backend-main` repo with the Phase 3 changes applied
directly (Admin/Ambassador auth, Ambassador hierarchy, Town Hall module).
Replace your local repo contents with this (or diff/merge file by file) and push.

## ⚠️ Do these steps before deploying, in order

### 1. Rotate your MongoDB password
Your `application.properties` had the live Atlas username/password committed in
plaintext (`donorly-admin` / `Donorly2026`). I removed it from the file and
replaced it with `${SPRING_MONGODB_URI}`, but **the old credentials are still
in your git history** — anyone with repo access can see old commits. Go to
MongoDB Atlas → Database Access → reset the `donorly-admin` password now,
then update the env var (see step 2) with the new connection string.

### 2. Set environment variables (local + Railway)
Local: create `src/main/resources/application-local.properties` (gitignored —
add it to `.gitignore`) or export env vars in your shell before running:
```
export SPRING_MONGODB_URI="mongodb+srv://donorly-admin:<NEW_PASSWORD>@donorly-cluster.ffxrhbj.mongodb.net/donorly?appName=donorly-cluster"
export JWT_SECRET="$(openssl rand -base64 48)"
```
Railway: add both `SPRING_MONGODB_URI` and `JWT_SECRET` under your service's
Variables tab, same as you did for the Mongo URI in Phase 2.

### 3. Migrate existing data — automated
A one-time migration endpoint is included: `MigrationController.java`. It
normalizes `role` to `"ADMIN"`/`"AMBASSADOR"` and BCrypt-hashes any password
that isn't already hashed, using the exact same `PasswordEncoder` your real
login endpoints use (so there's no risk of a hashing mismatch). It's
idempotent — safe to call more than once.

Steps:
1. Set `MIGRATION_SECRET` as an env var (Railway + local) to something
   random, e.g. `openssl rand -hex 16`.
2. Deploy this code, then call the endpoint once:
   ```
   curl -X POST "https://<your-backend-url>/api/admin/migrate-users?secret=<MIGRATION_SECRET>"
   ```
3. Read the JSON response — it lists exactly what changed per user. If any
   user shows `"role NOT changed"`, that means their existing role value
   wasn't recognized (not "admin" or "ambassador" in any casing) — fix that
   one manually in Atlas.
4. **Delete `MigrationController.java`**, remove the
   `"/api/admin/migrate-users"` line in `SecurityConfig.java`, remove the
   `migration.secret` line in `application.properties`, and redeploy. Leaving
   this endpoint live is a standing risk even with the secret.

Existing `Ambassador` documents don't need any manual backfill —
`ancestorPath` defaults to an empty list automatically for documents that
predate the field.


### 4. ⚠️ This will break the current frontend until it's updated
`SecurityConfig` now requires a valid JWT on every `/api/**` call except the
three login/logout endpoints. The current `donorly-portal` frontend calls
`apiGet`/`apiPost`/`apiPut` with no `Authorization` header at all, and its
"login" is really just an unauthenticated lookup against `/api/ambassadors`.
**Don't deploy this backend change until the frontend is updated to send the
JWT** (next step — I haven't built that yet, see below). If you need to test
the backend independently first, you can temporarily flip the requestMatcher
in `SecurityConfig` back to `permitAll()` while testing with curl/Postman.

## What changed — file by file

| File | What changed |
|---|---|
| `src/main/resources/application.properties` | Mongo URI moved to `${SPRING_MONGODB_URI}` env var; added `jwt.secret` / `jwt.expiration-ms` |
| `pom.xml` | Added `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.6) |
| `model/User.java` | Added `activeSessionToken`, `lastLoginAt` |
| `model/Ambassador.java` | Added `parentAmbassadorId`, `ancestorPath` |
| `repository/UserRepository.java` | Added `findByEmailAndRole` |
| `repository/AmbassadorRepository.java` | Added `findByParentAmbassadorId`, `findByAncestorPathContains` |
| `service/AmbassadorService.java` | Added `createSubAmbassador`, `createRootAmbassador`, `getDownline`, `isSelfOrAncestor`, `deactivate` |
| `controller/AmbassadorController.java` | Added `/sub-ambassadors`, `/root`, `/downline`, `/deactivate` endpoints alongside existing CRUD |
| `config/SecurityConfig.java` | Replaced `permitAll()` with JWT-based auth; added `PasswordEncoder` bean (BCrypt) |
| `security/JwtUtil.java` | **New** — issues/parses JWTs |
| `security/JwtAuthFilter.java` | **New** — validates JWT on each request, enforces single-session for ambassadors |
| `controller/AuthController.java` | **New** — `/api/auth/admin/login`, `/api/auth/ambassador/login`, `/api/auth/logout` |
| `model/TownHall.java` | **New** |
| `repository/TownHallRepository.java` | **New** |
| `service/TownHallService.java` | **New** |
| `controller/TownHallController.java` | **New** — full CRUD at `/api/townhalls` |

## Design decisions baked in (confirmed earlier in chat)

- Single-session login **blocks** a second concurrent login (409 response)
  rather than silently logging the first session out.
- Only **Admin** can create root-level (no-parent) ambassadors via
  `POST /api/ambassadors/root`. Ambassadors create sub-ambassadors under
  themselves via `POST /api/ambassadors/{id}/sub-ambassadors`.
- Deactivating an ambassador does **not** reassign their donors/pledges.
- Town Hall is a fully separate collection from the existing `Event` entity.

## Not included in this patch (flag if you want these now)

- Read-only enforcement on a deactivated ambassador's Donor/PledgeCard data
  (currently they're just marked `status: "inactive"`, nothing blocks edits).
- The frontend (`donorly-portal`) — needs real login forms wired to
  `/api/auth/admin/login` / `/api/auth/ambassador/login`, JWT attached to
  every API call, a Town Hall tab, Google Places autocomplete, and a
  downline/"My Ambassadors" view. This is the next piece of work.
