import { useEffect } from 'react'
import { sessionApi } from '../api/sessionApi'
import type { Action } from '../state/simulationReducer'
import type { FailedEvent, FinishedEvent, MoveEvent } from '../api/types'

/**
 * Subscribes to a session's progress stream. The browser's EventSource reconnects on
 * its own and the server replays history on every subscribe, so a dropped connection
 * costs nothing; the reducer de-duplicates by move number. After a terminal event the
 * server closes the stream and we must close too, or the browser would reconnect forever.
 */
export function useSessionEvents(sessionId: string | null, dispatch: (action: Action) => void): void {
  useEffect(() => {
    if (sessionId === null) {
      return
    }
    const source = new EventSource(sessionApi.eventsUrl(sessionId))
    const close = () => source.close()

    source.addEventListener('move', (message) => {
      dispatch({ type: 'move', event: JSON.parse(message.data) as MoveEvent })
    })
    source.addEventListener('finished', (message) => {
      dispatch({ type: 'finished', event: JSON.parse(message.data) as FinishedEvent })
      close()
    })
    source.addEventListener('failed', (message) => {
      dispatch({ type: 'simulationFailed', event: JSON.parse(message.data) as FailedEvent })
      close()
    })
    source.onerror = () => {
      // While the browser is still retrying, the replay on reconnect covers any gap.
      // Only when it has given up do we fall back to reading the session directly.
      if (source.readyState !== EventSource.CLOSED) {
        return
      }
      sessionApi
        .get(sessionId)
        .then((details) => dispatch({ type: 'hydrate', details }))
        .catch((error: unknown) =>
          dispatch({ type: 'requestFailed', during: 'reload', code: 'NETWORK', message: String(error) }),
        )
    }

    return close
  }, [sessionId, dispatch])
}
