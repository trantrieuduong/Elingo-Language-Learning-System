---
paths:
  - "src/main/java/**/*.java"
---

# Logging rules (Elingo backend, Spring Boot)

Follow these rules whenever you write, edit, or review Java code that logs.

## 1. Logger declaration

- Use Lombok `@Slf4j` with an explicit topic: `@Slf4j(topic = "UPPER-KEBAB-NAME")`.
- The topic mirrors the class name in upper kebab case: `AuthService` -> `AUTH-SERVICE`, `AuthenticationController` -> `AUTHENTICATION-CONTROLLER`.
- Do not rename existing topics and do not switch to plain `@Slf4j`.
- Never add per-topic levels to `application-*.yml`. Root is INFO and only third-party packages are lowered.

## 2. Context is automatic: never add it by hand

- `requestId` and `userId` are put in MDC by `RequestLoggingFilter` / `JwtAuthenticationFilter`.
- Do not write `requestId` or `userId` inside log messages.
- Do not call `MDC.clear()` outside `RequestLoggingFilter`.
- Any new `Executor` used with `@Async` must call `executor.setTaskDecorator(new MdcTaskDecorator())`, otherwise logs lose `requestId`.

## 3. Message format

`<Short event in English> key=value key=value`

```java
log.info("Lesson completed lessonId={} score={}", lessonId, score);
log.warn("Login failed reason={}", reason);
log.error("Email sending failed templateId={}", templateId, ex);
```

- Always use `{}` placeholders. Never concatenate strings.
- State the event first, then the data. The event is a short past-tense phrase, not a full sentence.
- Use `key=value` pairs with camelCase keys, so logs are searchable.
- Messages are in English, consistent across the codebase.
- Log ids and counts, not whole objects. Never log an entity or DTO directly (`toString()` may expose passwords or personal data).
- Expensive debug arguments go behind `if (log.isDebugEnabled())`.
- Do not log inside large loops. Log one summary after: `processed=1000 failed=3`.

## 4. Levels

| Level | Use for |
|---|---|
| ERROR | Unexpected failure that needs a human (500, lost DB connection, external service down). Always include the exception. |
| WARN | Client-caused or recoverable problems: business errors (4xx), rejected JWT, retry, fallback. |
| INFO | Meaningful business events: user registered, lesson completed, job started/finished. |
| DEBUG | Developer detail. Never rely on it in prod. |

- `AppException` (business error) is WARN, never ERROR.
- Validation failures are INFO.

## 5. Where to log

- **Controllers: do not log.** The access log in `RequestLoggingFilter` already records method, path, status, and duration.
- **Services:** log important business events at INFO.
- **Exceptions: throw, do not log-and-throw.** Each exception is logged exactly once, in `GlobalExceptionHandler` or `HttpExceptionHandler`.
- Swallowing an exception on purpose (a side job such as a reminder email) is the only case where a service logs it directly, with WARN and full context.
- Pass the exception as the last argument (`log.error("...", ex)`). Never use `ex.getMessage()` alone or `printStackTrace()`.
- Every new handler in the exception handlers must log with the right level and add the `requestId` automatically (the response advice handles `ApiResponse`; do not set it by hand).

## 6. Sensitive data: never log

- Passwords, JWTs, refresh tokens, OTPs, API keys, secrets.
- Request or response bodies, and validation `rejectedValue` (Spring's default validation log prints it; keep `ExceptionHandlerExceptionResolver` at ERROR and log only field names):

```java
log.info("Validation failed fields={}",
        ex.getBindingResult().getFieldErrors().stream().map(FieldError::getField).toList());
```

- Malformed JSON errors: log the exception class name, not `ex.getMessage()`.

## 7. Configuration facts (do not break)

- No `logback-spring.xml`. Logging is configured only through `application.yml`, `application-dev.yml`, `application-prod.yml`.
- `prod`: JSON to stdout (`logging.structured.format.console: logstash`). No log files in containers.
- `dev`: readable text pattern with `%X{requestId}` and `%X{userId}`.
- A new logger needs no YAML change.

## 8. Review checklist

Before finishing code that touches logging, verify:

- [ ] Topic is set and follows the naming rule
- [ ] No string concatenation, no `requestId`/`userId` in message text
- [ ] Level matches section 4
- [ ] No controller logging, no log-and-throw
- [ ] No sensitive data, no whole DTO or entity logged
- [ ] Exception passed as the last argument when an error is logged