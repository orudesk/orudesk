# @orudesk/api-client

A typed client for the Orudesk API, generated from the API's OpenAPI contract.

```
openapi/orudesk-standard.json   ← a copy of the contract (from the API's OpenApiContractTest)
src/generated/schema.ts         ← TypeScript types, written by openapi-typescript (never edit)
src/index.ts                    ← createOrudeskClient(): openapi-fetch with those types
```

```ts
import { createOrudeskClient } from '@orudesk/api-client';

const api = createOrudeskClient();
const { data, error } = await api.GET('/api/v1/people', { params: { query: { q: 'asha' } } });
```

## When the API changes

```bash
cd api && ./gradlew :apps:orudesk-standard:test --tests '*OpenApiContractTest'
cd ../web/packages/api-client && pnpm contract:sync    # copies the contract, regenerates the types
```

Commit both files. CI fails if either is out of date, so the web app never compiles against an API that
no longer exists, and every API change shows up in the pull request as a diff of the contract.
