# Maestro E2E Tests

## Install

```bash
brew tap mobile-dev-inc/tap
brew install maestro
```

## Run single test

```bash
maestro test maestro-tests/aryamahasangh/app-smoke.yaml
```

## Run full smoke suite

```bash
maestro test maestro-tests/aryamahasangh/smoke-suite.yaml
```

## Tips

- Prefer Hindi text selectors present in UI to keep tests stable.
- Avoid absolute coordinates except for opening the drawer (top-left) if needed.
- If a step becomes flaky, add brief `- scroll` or `- waitForAnimationToEnd` before assertions.
- For CRUD flows, assert GlobalMessageManager Hindi messages (e.g., "आर्य समाज सफलतापूर्वक जोड़ा गया").
- Ensure your app points to a stable environment with predictable data for repeatable tests.
