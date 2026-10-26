# Contributing to Orudesk

Thank you for helping. This page says how to get a change merged with as little back-and-forth as possible.

## Before you start

- **Small fix** (a typo, an obvious bug): open a pull request directly.
- **Anything bigger** (a feature, a new module, a change to an API or a table): open an issue first and
  describe the problem. It saves you from writing code that goes in a different direction.

## Set up

Follow the quick start in the [README](README.md). `./gradlew test` must pass before you start, so you
know any failure afterwards is yours.

## Making a change

1. Fork the repository and create a branch from `main`: `fix/people-search-paging`, `feat/donor-receipts`.
2. Keep the structure: business rules in `domain/`, use cases in `application/`, HTTP, database and
   Redis code in `adapter/`. A module uses another module only through its public API (Spring Modulith's
   `ModularityTest` fails otherwise).
3. **Tests are part of the change.** A bug fix comes with a test that failed before the fix. New rules get
   unit tests; new endpoints get a web slice test; anything touching SQL gets a Testcontainers test.
4. **Database changes are new migrations.** Add `V<next>__what_it_does.sql` in your module's
   `db/migration/<module>/` folder. Never edit a migration that has been merged: other databases have
   already run it.
5. No secrets in git. Configuration comes from environment variables; add placeholders to
   `api/.env.example`.

## Commit messages

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(party): search people by phone number
fix(identity): refresh tokens survive a Redis restart
docs: explain org units in the README
```

The type (`feat`, `fix`, `docs`, `test`, `build`, `refactor`) and the module in brackets make the changelog
almost write itself.

## Sign your commits (DCO)

Every commit must carry a `Signed-off-by` line, which certifies the
[Developer Certificate of Origin](https://developercertificate.org/): that you wrote the change, or have
the right to submit it under the project's licence. Git adds the line for you:

```bash
git commit -s -m "fix(party): ..."
```

## Pull requests

- One topic per pull request. Two unrelated fixes are two pull requests.
- Fill in the template: what changed, why, and how you tested it.
- CI must be green. A maintainer reviews; please answer or resolve every comment.

## Code of conduct

Everyone taking part is expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
