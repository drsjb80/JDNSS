# Changelog

All notable changes to this project are documented in this file.

## v3.0 - 2026-05-29

Baseline for this summary: historical 2.1 version commit 2a51d34 (no v2.1.0 tag exists in the repository history).

### Highlights

- Refactored core DNS response and parser flows for readability, safer branching, and easier testing.
- Greatly expanded unit and integration coverage across response assembly, parser behavior, transport threads, HTTPS handling, and utility code paths.
- Added and hardened DNS-over-HTTPS support, including JSON-style queries and improved binary request/response handling.
- Improved robustness for EDNS/OPT cookie processing, including malformed-cookie handling without assertion crashes.
- Migrated legacy system shell test cases into JUnit-based integration tests and removed the old shell harness.
- Modernized CI/CD from Travis to GitHub Actions with CI, CD, and release automation.

### Detailed Changes

#### DNS and protocol behavior

- Improved NXDOMAIN/refused and response-code handling in multiple query/response paths.
- Reworked CNAME fallback and DNSSEC response guard logic for clearer behavior and safer section assembly.
- Improved UDP payload and truncation handling through centralized overflow checks.
- Added safer network binding and IP parsing validation for IPv4/IPv6 inputs.

#### DNSSEC and record handling

- Continued DNSSEC support hardening for RRSIG, DNSKEY, and NSEC-related flows.
- Improved authority/additional section record construction and dispatch logic.
- Fixed and tested RRCode lookup and record-specific edge cases.

#### DoH, TLS, and HTTPS

- Added DNS-over-HTTPS JSON endpoint behavior for name/type queries.
- Improved binary DoH handling and content-type behavior.
- Added SSL context and HTTPS handler test coverage, including failure scenarios.
- Expanded coverage around TLS and transport initialization paths.

#### Testing and coverage

- Added JaCoCo coverage tracking and systematically increased coverage in targeted modules.
- Added focused tests for parser directives, response invariants, thread loops, datagram formatting, cookie hashing, and startup wiring.
- Migrated broad system-case coverage to JUnit integration tests for maintainability and repeatability.

#### Tooling and release engineering

- Removed Travis configuration and migrated to GitHub Actions CI/CD workflows.
- Added release automation for tag-driven builds and artifact publication.
- Added Node 24-ready workflow action versions.
- Added automated release-note generation and changelog refresh workflow.
- Bumped project version from 2.1 lineage to 3.0 for release.

### Notable Commit Themes in the 2.1 -> 3.0 window

- Response and parser refactors, testability improvements, and branch simplification.
- Coverage expansion and regression tests for edge-case protocol handling.
- DoH feature completion and polish.
- CI/CD modernization and release-process automation.
