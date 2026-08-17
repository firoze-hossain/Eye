# TrackEye

**TrackEye** is a self-hosted employee activity monitoring platform for organizations that need a clear, centralized view of device activity, productivity trends, and policy events. It combines a lightweight desktop agent with a secure central API and a browser-based management dashboard.

> Use TrackEye transparently and only with appropriate employee notice, consent, and a lawful monitoring policy. Requirements vary by jurisdiction.

## What is available today

- **Cross-platform desktop agent** for Windows, macOS, and Linux, with automatic start at user login.
- **Activity tracking** for the active application, window title, active/idle time, and AFK sessions.
- **Browser activity capture** including browser, page title, and URL. Linux can read the active address bar through the standard AT-SPI accessibility interface, including private windows where supported.
- **Screenshot capture** on a five-minute interval and on application switches, with multi-monitor support.
- **Central synchronization** that retains activity, browser events, AFK sessions, and screenshots for connected devices.
- **Live operations view** for online status, current application/window, and a continuously refreshed activity feed.
- **Live screen watch**: authorized managers can initiate a short-lived viewing session for a connected device.
- **Employee and device administration**: create invitations, assign roles/managers, generate device-registration tokens, activate/deactivate staff, and pause, resume, or revoke devices.
- **Dashboard and reporting**: organization metrics, productivity score, active users, application summaries, weekly reports, and CSV export.
- **Policy alerts**: organization-defined URL, keyword, application, and window-title rules; matched activity creates notifications for relevant admins and managers.
- **Authentication and tenant isolation**: token-based dashboard access, per-device API keys, request rate limiting, and organization-scoped screenshot access.

## Screenshots

Replace the four files below with approved product screenshots. The paths are intentional placeholders so repository images can be added without rewriting this README.

| Dashboard | Live activity |
| --- | --- |
| ![Dashboard overview — placeholder](docs/images/dashboard-overview.png) | ![Live activity — placeholder](docs/images/live-activity.png) |

| Employee details | Reports and screenshots |
| --- | --- |
| ![Employee details — placeholder](docs/images/employee-details.png) | ![Reports and screenshots — placeholder](docs/images/reports-screenshots.png) |

Recommended image size: 1600 × 1000 px or larger. Avoid screenshots containing real employee data, tokens, URLs, or personally identifiable information.

## Architecture

```text
TrackEye desktop agent  ── activity, browser data, AFK sessions, screenshots ──►  TrackEye Central API
       Windows / macOS / Linux                                                        │
                                                                                PostgreSQL or H2
                                                                                       │
TrackEye web dashboard  ◄──── authenticated administration, reports, live views ──────┘
```

## Technology stack

| Layer | Technology |
| --- | --- |
| Desktop agent | Java 21, Spring Boot 3, Swing/FlatLaf, JNA, SQLite |
| Central API | Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, Flyway, Spring Security Crypto, Caffeine |
| Data storage | PostgreSQL for deployment; H2 file database for local development; filesystem screenshot storage |
| Web dashboard | Next.js 14, React 18, TypeScript, Tailwind CSS, NextAuth, Axios, React Query, Recharts |

## Repository layout

```text
TrackEye/                     Desktop monitoring agent
trackeye-central/             Central API, persistence, policy engine, and sync endpoints
trackeye-central-frontend/    Next.js administration dashboard
docs/images/                  README screenshot locations
```

## Run locally

### Prerequisites

- JDK 21
- Node.js 18.17+ and npm
- Maven (or use the included Maven wrapper)
- PostgreSQL for production deployments; local development defaults to H2

### 1. Start the central API

```bash
cd trackeye-central
./mvnw spring-boot:run
```

The development profile listens on `http://localhost:8080`, stores H2 data under `trackeye-central/data`, and writes screenshots to `trackeye-central/data/screenshots`.

Before exposing the service outside local development, set strong `JWT_SECRET` and `ENCRYPTION_KEY` environment variables, configure PostgreSQL, and update the allowed dashboard origins in `src/main/resources/application-dev.yml` (or your production configuration).

### 2. Start the dashboard

Create `trackeye-central-frontend/.env.local`:

```dotenv
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=replace-with-a-long-random-secret
```

Then run:

```bash
cd trackeye-central-frontend
npm install
npm run dev
```

Open `http://localhost:3000` and register or sign in through the dashboard.

### 3. Build and connect the desktop agent

```bash
cd TrackEye
mvn clean package -DskipTests
java -jar target/TrackEye-2.0.0.jar
```

The agent opens its setup page at `http://localhost:8765/setup.html`. From the dashboard, generate a device token and enter it with the employee’s email to connect the device.

For staff deployment and login-start setup, see [the agent installation guide](TrackEye/install/README-INSTALL.md).

### Platform notes

- **macOS:** allow Screen Recording permission for Java to enable screenshots.
- **Linux:** browser address-bar capture uses AT-SPI. Install `at-spi2-core` when it is not already available.
- **Windows, macOS, and Linux:** distribution scripts are included; production installers and code signing are not yet included.

## Security and privacy considerations

TrackEye processes sensitive workforce data. A production rollout should at minimum use HTTPS, keep secrets out of source control, restrict CORS origins, back up and encrypt retained data, configure retention/deletion policies, limit administrator access, and document the employee-notice and consent process. Screenshot and activity data should never be committed to this repository.

## Roadmap

The following items are planned directions, not currently released functionality:

- Signed native installers and uninstallers (`.msi`, `.dmg`, `.deb`) built with `jpackage`.
- Production deployment templates, health monitoring, backups, and guided PostgreSQL configuration.
- Configurable data retention, export controls, and additional privacy controls.
- Richer report formats, scheduled delivery, and organization-level analytics.
- Live-watch transport improvements beyond the current short-lived polling implementation.
- Expanded automated test coverage and CI/CD release workflows.

## Contributing

Contributions are welcome. Keep changes scoped to one of the three applications, include relevant tests where practical, and do not add captured activity, screenshots, credentials, or local databases to commits.

## License

No license is currently declared for this repository. Add a `LICENSE` file before distributing or accepting external contributions under specific terms.
