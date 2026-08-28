import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError, sessionApi } from './sessionApi'

function reply(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const session = { sessionId: 's1', state: 'CREATED', game: {}, moves: [], failureReason: null }

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

    const pending = sessionApi.create()
    await vi.runAllTimersAsync()

    await expect(pending).resolves.toMatchObject({ sessionId: 's1' })
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('does not retry a rejected request', async () => {
    const fetchMock = vi.fn().mockResolvedValue(reply(409, { code: 'SIMULATION_ALREADY_STARTED', message: 'already' }))
    vi.stubGlobal('fetch', fetchMock)

    const pending = sessionApi.simulate('s1')
    await vi.runAllTimersAsync()

    await expect(pending).rejects.toMatchObject({ code: 'SIMULATION_ALREADY_STARTED', status: 409 })
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
