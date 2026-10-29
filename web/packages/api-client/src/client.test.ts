import { describe, expect, expectTypeOf, it, vi } from 'vitest';
import { createOrudeskClient, type Schemas } from './index';

/** A fetch that records the request and answers with the given JSON. */
function fakeFetch(body: unknown, status = 200) {
  const requests: Request[] = [];
  const fetch = vi.fn(async (request: Request) => {
    requests.push(request);
    return new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    });
  });
  return { fetch, requests };
}

describe('createOrudeskClient', () => {
  it('fills in path parameters and returns typed data', async () => {
    const person: Schemas['PersonDetails'] = {
      id: '0e9d9b1c-6a3f-4b8e-9d55-0f4a0d7f5a11',
      firstName: 'Asha',
      lastName: 'Rao',
      dateOfBirth: null,
      phone: null,
      email: 'asha@example.org',
      version: 0,
    };
    const { fetch, requests } = fakeFetch(person);
    const client = createOrudeskClient({ baseUrl: 'http://api.test', fetch });

    const { data, error } = await client.GET('/api/v1/people/{personId}', {
      params: { path: { personId: person.id } },
    });

    expect(requests[0]?.url).toBe(`http://api.test/api/v1/people/${person.id}`);
    expect(error).toBeUndefined();
    expect(data?.firstName).toBe('Asha');
  });

  it('puts query parameters in the URL', async () => {
    const { fetch, requests } = fakeFetch({ items: [], nextCursor: null });
    const client = createOrudeskClient({ baseUrl: 'http://api.test', fetch });

    await client.GET('/api/v1/people', { params: { query: { q: 'asha', size: 10 } } });

    expect(requests[0]?.url).toBe('http://api.test/api/v1/people?q=asha&size=10');
  });

  it('returns the error body instead of throwing when the API says no', async () => {
    const { fetch } = fakeFetch({ title: 'Forbidden', status: 403 }, 403);
    const client = createOrudeskClient({ baseUrl: 'http://api.test', fetch });

    const { data, error, response } = await client.GET('/api/v1/users/me');

    expect(data).toBeUndefined();
    expect(response.status).toBe(403);
    expect(error).toEqual({ title: 'Forbidden', status: 403 });
  });

  it('knows from the contract which fields are always there', () => {
    // These checks run in the type checker: if the contract changes, `pnpm typecheck` fails here.
    expectTypeOf<Schemas['PersonDetails']['id']>().toEqualTypeOf<string>();
    expectTypeOf<Schemas['PersonDetails']['phone']>().toEqualTypeOf<string | null | undefined>();
    expectTypeOf<Schemas['PersonPage']['nextCursor']>().toEqualTypeOf<string | null | undefined>();
  });
});
