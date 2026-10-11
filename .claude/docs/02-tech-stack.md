# Tech Stack

> Source of truth: `pom.xml` (backend) and `../2s-tw-smart-booking-frontend/package.json` (frontend).
> "Used?" = whether the code actually uses it today. Several dependencies are declared but not used.

## Backend — `2s-tw-smart-booking-backend`

| Layer | Technology | Version | Used? | Where |
|---|---|---|---|---|
| Language | Java | 17 | ✅ | `pom.xml` `<java.version>` |
| Framework | Spring Boot | 3.5.7 | ✅ | parent POM |
| Web / REST | spring-boot-starter-web (Spring MVC, Tomcat) | managed | ✅ | `controller/*` |
| Validation | spring-boot-starter-validation (Jakarta Bean Validation) | managed | ✅ | `dto/request/*` |
| Persistence | Spring Data JPA + Hibernate | managed | ✅ | `entity/*`, `repository/*` |
| Database | MySQL (hosted on **TiDB Cloud**, which speaks MySQL) | connector 8.0.33 | ✅ | `application.yml` (+ `application-{local,dev}.yml`) |
| Schema mgmt | **Liquibase** (formatted SQL changelogs) + `ddl-auto=validate` | 4.31 (Boot-managed) | ✅ | `src/main/resources/db/changelog/` — see [09](./09-database-migrations.md) |
| Security | Spring Security (stateless) | managed | ✅ | `config/SecurityConfig.java` |
| Tokens | JJWT (`io.jsonwebtoken`) HS512 | 0.11.5 | ✅ | `security/JwtTokenProvider.java` |
| Social login | spring-boot-starter-oauth2-client (Google, Facebook) | managed | ✅ | `security/oauth2/*` |
| Password hash | BCrypt | — | ✅ | `SecurityConfig.passwordEncoder()` |
| Email | spring-boot-starter-mail (Gmail SMTP) + **Thymeleaf** HTML templates | 3.1.3 | ✅ | `EmailServiceImpl`, `resources/templates/email/*.html` |
| File storage | AWS SDK v2 S3 client → **Cloudflare R2** | BOM 2.25.20 | ✅ | `config/R2Config.java`, `FileStorageService` |
| AI | Google **Gemini** REST API (`gemini-flash-latest`) | — | ✅ | `service/ai/AiService.java` |
| Scheduling | Spring `@Scheduled` (`@EnableScheduling`) | — | ✅ | `scheduler/*` |
| Logging | SLF4J/Logback + **Spring AOP** aspect, MDC request id | managed | ✅ | `logging/*` — see [11](./11-logging.md) |
| i18n (backend) | Spring `MessageSource` (`i18n/messages*`, `i18n/emails*`), `Accept-Language` | managed | ✅ | `i18n/*` — see [10](./10-i18n.md) |
| Boilerplate | Lombok | 1.18.40 | ✅ | `@Data`, `@RequiredArgsConstructor` … |
| Env loading | dotenv-java (reads `.env` via custom `EnvironmentPostProcessor`) | 3.0.0 | ✅ | `config/DotenvEnvironmentPostProcessor.java`, `META-INF/spring.factories` |
| API docs | swagger-annotations only (no springdoc UI dependency) | 2.2.21 | ⚠️ annotations only | — |
| Redis | spring-boot-starter-data-redis | managed | ❌ declared, unused | — |
| WebSocket | spring-boot-starter-websocket | managed | ❌ declared, unused | — |
| Quartz | spring-boot-starter-quartz | managed | ❌ unused (schedulers use plain `@Scheduled`) | — |
| WebFlux | spring-boot-starter-webflux — only for `WebClient` to call Gemini | managed | ✅ (client only) | `AiService` |
| Tests | spring-boot-starter-test | managed | ⚠️ one smoke test only | `src/test/...` |
| Build | Maven (wrapper `mvnw`) | — | ✅ | |
| Container | Multi-stage Dockerfile (maven:3.9.6 → temurin 17 JRE alpine), `-Xmx350m`, SerialGC | — | ✅ | `Dockerfile` |
| CI/CD | `.github/workflows/ci.yml`, `cd.yml` | — | ❌ **both files are empty** | |

### Runtime configuration (env vars)

All secrets come from environment variables (or a local `.env`). Names, never values:

`SERVER_PORT` (default 8080), `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER_NAME`, `DB_PASSWORD`, `JWT_SECRET`, `FRONTEND_URL`, `GOOGLE_CLIENT_ID/SECRET`, `FACEBOOK_CLIENT_ID/SECRET`, `MAIL_USERNAME/PASSWORD`, `GEMINI_API_KEY`, `R2_ACCESS_KEY_ID`, `R2_SECRET_KEY`, `R2_ENDPOINT`, `R2_BUCKET_NAME`, `FILE_UPLOAD_DIR`.

Key settings: JWT expiry = `86400000` ms (24h); multipart limit 10 MB; Hibernate timezone `Asia/Ho_Chi_Minh`; physical naming = **standard** (column names = Java field names, e.g. `checkInDate`, not `check_in_date`).

## Frontend — `2s-tw-smart-booking-frontend`

| Concern | Technology | Version | Notes |
|---|---|---|---|
| Language | JavaScript (JSX), **no TypeScript** | — | |
| UI library | React | 18.3 | `src/main.jsx` uses `StrictMode` |
| Build / dev server | Vite | 7 | `vite.config.js`: alias `@` → `src`, proxy `/api` → `localhost:8386` |
| Routing | React Router | 7.9 | route arrays in `src/routes/*Routes.jsx` |
| Global state | **React Context** (`AuthContext`, `NotificationContext`, …) | — | Redux Toolkit is installed but `src/store/**` files are **all 0 bytes** |
| HTTP | axios | 1.13 | single instance in `src/services/axios.config.js` with request/response interceptors |
| Styling | Tailwind CSS 3 + some plain `.css` | 3.4 | |
| Forms | react-hook-form + yup | | |
| Maps | Mapbox GL / react-map-gl (Explore page), Leaflet, Google Maps API | | three map libs installed |
| Charts | Recharts | 3 | dashboards, price forecast |
| i18n | i18next / react-i18next — Vietnamese (default) + English, every page (customer, owner, admin) | | `src/i18n/locales/{vi,en}/*.json` — see [10](./10-i18n.md) |
| Dates | date-fns, dayjs, moment (all three!) | | |
| Animation | framer-motion | | |
| Misc | qrcode.react (fake payment QR), react-easy-crop (avatar), @dnd-kit (image sort), react-hot-toast | | |
| Testing | vitest + playwright installed; no meaningful tests | | |
| Deploy | Vercel (backend CORS allows `https://*.vercel.app`) | | |

### Frontend env vars

`.env` defines `VITE_API_BASE_URL`, `VITE_MAPBOX_TOKEN`. **But** `services/axios.config.js` reads `VITE_REACT_APP_API_URL` — see [07-gotchas-and-risks.md](./07-gotchas-and-risks.md).

## Further reading
- Spring Boot reference — https://docs.spring.io/spring-boot/index.html
- Vite env variables — https://vite.dev/guide/env-and-mode
