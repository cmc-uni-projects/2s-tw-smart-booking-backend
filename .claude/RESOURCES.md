# Smart Booking System — Resources

## Knowledge — the codebase itself (primary source #1)
- [docs/](./docs/) — project documentation distilled from the source (tech stack, architecture, domain, flows, API, gotchas). Always re-verify against code; line numbers drift.

## Knowledge — official docs (all links checked 2026-10-10)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/index.html)
  Auto-configuration, `application.properties`, starters. Use for: config questions, how the app boots.
- [Spring Security — Servlet Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
  FilterChain, SecurityContextHolder, how a filter like `JwtAuthenticationFilter` plugs in. Use for: anything auth/role related.
- [Spring Security — OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html)
  Use for: the Google/Facebook login flow.
- [Spring Guide: Securing a Web Application](https://spring.io/guides/gs/securing-web)
  Short hands-on intro. Use for: first contact with `SecurityFilterChain`.
- [Spring Data JPA — Query Methods](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
  How `findByUserUserId` / `@Query` become SQL. Use for: repositories.
- [Spring Framework — @Transactional](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
  Use for: why `submitPayment` rolls back when a promo limit is hit.
- [Spring Framework — Task Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
  Use for: `@Scheduled` jobs (auto-cancel, promo expiry).
- [JJWT (GitHub README)](https://github.com/jwtk/jjwt)
  The JWT library used. Use for: token structure, signing, parsing.
- [React — Learn](https://react.dev/learn) and [useContext](https://react.dev/reference/react/useContext)
  Use for: components, state, the `AuthContext` pattern.
- [React Router](https://reactrouter.com/home)
  Use for: route arrays, `ProtectedRoute`, `Navigate`.
- [axios — Interceptors](https://axios-http.com/docs/interceptors)
  Use for: how the token is attached and 401s are handled.
- [Vite — Env Variables and Modes](https://vite.dev/guide/env-and-mode)
  Use for: the `VITE_*` env-var mismatch.

## Kubernetes
See [k8s-course/RESOURCES.md](./k8s-course/RESOURCES.md).

## Wisdom (Communities)
- [Stack Overflow — `spring-boot` / `spring-security` / `reactjs` tags](https://stackoverflow.com/questions/tagged/spring-security)
  Use for: concrete, reproducible errors.
- [r/SpringBoot](https://www.reddit.com/r/SpringBoot/) and [r/reactjs](https://www.reddit.com/r/reactjs/)
  Use for: design questions ("how should I enforce ownership checks?"), code-review style feedback.
- Your project teammates (original repo `bquang016/Smart_Booking_System_Backend`) — the best source for *why* something was built a certain way.

## Gaps
- No real payment-gateway docs needed yet (payment is simulated). Add VNPay/MoMo sandbox docs if the mission grows to real payments.
