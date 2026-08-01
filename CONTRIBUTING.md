# Contributing

Thanks for your interest in improving in-memory-pubsub! Contributions of all
kinds are welcome — bug reports, feature ideas, docs, and code.

## Getting started

1. Fork and clone the repository.
2. Make sure you have **JDK 21+** and **Maven 3.9+** installed.
3. Build and run the tests:

   ```bash
   mvn clean test
   ```

## Making changes

- Create a topic branch: `git checkout -b feat/short-description`.
- Keep changes focused; one logical change per pull request.
- Add or update tests for any behavior change.
- Run `mvn clean verify` before pushing.

## Coding style

- Follow standard Java conventions (4-space indentation, one public class per file).
- Public APIs must have Javadoc.
- Prefer SLF4J logging over `System.out`/`System.err`.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/), e.g.:

- `feat: add DLQ inspection API`
- `fix: prevent dispatcher leak on shutdown`
- `docs: clarify backpressure behavior`

## Pull requests

- Fill out the pull request template.
- Ensure CI is green.
- Link any related issues.

By contributing, you agree that your contributions are licensed under the
project's [MIT License](LICENSE).
