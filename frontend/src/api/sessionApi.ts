import type { ApiError, MatchSettings, SessionDetails } from './types'

const BASE = '/api/sessions'

export class ApiRequestError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message)
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(BASE + path, { headers: { Accept: 'application/json' }, ...init })
  } catch {
    throw new ApiRequestError(0, 'NETWORK', 'Could not reach the server')
  }
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiError | null
    throw new ApiRequestError(response.status, body?.code ?? 'HTTP_' + response.status, body?.message ?? response.statusText)
  }
  return (await response.json()) as T
}

export const sessionApi = {
  create: (settings: MatchSettings) =>
    request<SessionDetails>('', {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify(settings),
    }),
  simulate: (sessionId: string) => request<unknown>(`/${sessionId}/simulate`, { method: 'POST' }),
  get: (sessionId: string) => request<SessionDetails>(`/${sessionId}`),
  eventsUrl: (sessionId: string) => `${BASE}/${sessionId}/events`,
}
