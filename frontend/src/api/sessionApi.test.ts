import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError, sessionApi } from './sessionApi'

function reply(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const session = { sessionId: 's1', state: 'CREATED', game: {}, moves: [], failureReason: null }
const settings = { strategy: 'HEURISTIC', blunderRate: 0.25 } as const

describe('sessionApi', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('retries once when the edge has no route yet', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(reply(503, {})).mockResolvedValueOnce(reply(201, session))
    vi.stubGlobal('fetch', fetchMock)

    const pending = sessionApi.create(settings)
    await vi.runAllTimersAsync()

    await expect(pending).resolves.toMatchObject({ sessionId: 's1' })
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('sends the match settings as JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(reply(201, session))
    vi.stubGlobal('fetch', fetchMock)

    await sessionApi.create({ strategy: 'RANDOM', blunderRate: 0 })

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/sessions')
    expect(init.method).toBe('POST')
    expect(JSON.parse(init.body as string)).toEqual({ strategy: 'RANDOM', blunderRate: 0 })
    expect((init.headers as Record<string, string>)['Content-Type']).toBe('application/json')
  })

  it('does not retry a rejected request', async () => {
    const fetchMock = vi.fn().mockResolvedValue(reply(409, { code: 'SIMULATION_ALREADY_STARTED', message: 'already' }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(sessionApi.simulate('s1')).rejects.toMatchObject({ code: 'SIMULATION_ALREADY_STARTED', status: 409 })
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('reports a network failure after the second attempt fails too', async () => {
    const fetchMock = vi.fn().mockRejectedValue(new TypeError('offline'))
    vi.stubGlobal('fetch', fetchMock)

    const pending = sessionApi.get('s1')
    pending.catch(() => undefined)
    await vi.runAllTimersAsync()

    await expect(pending).rejects.toBeInstanceOf(ApiRequestError)
    await expect(pending).rejects.toMatchObject({ code: 'NETWORK' })
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
