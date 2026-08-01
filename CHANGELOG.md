# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Repackaged all sources under `com.rahilsh.pubsub` (`api`, `core`, `subscriber`).
- Replaced `System.out`/`System.err` usage with SLF4J logging.
- Added Javadoc to all public APIs and input validation to `Topic` and `PubSubBroker`.
- Inverted control of concurrency: `Topic` now receives its buffer via a
  `Supplier<BlockingQueue<T>>` and its dedicated single-thread `ExecutorService`
  via injection instead of constructing them internally.
- `PubSubBroker` mints one dedicated single-thread dispatcher per topic (via an
  injected `Supplier<ExecutorService>`); delivery runs on a simple blocking
  `take()` loop that keeps messages ordered without any drain-guard bookkeeping.
- `PubSubBroker` gained a DI constructor plus `createTopic` overloads
  (default, capacity, and custom queue factory).

### Added
- MIT `LICENSE`.
- `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, and this changelog.
- Issue and pull request templates.
- Dependabot configuration for Maven and GitHub Actions.
- JaCoCo code coverage reporting with a 90% line-coverage gate (currently ~96%).
- Comprehensive unit tests (`TopicTest`, `PubSubBrokerTest`, `SubscriberTest`)
  covering delivery, fan-out, ordering, retries, dead-lettering, backpressure,
  exception recovery, and validation.

### Removed
- Committed `output.log` build artifact.
