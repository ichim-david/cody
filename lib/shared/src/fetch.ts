import type { Agent } from 'node:http'

/**
 * By hard-requiring isomorphic-fetch, we ensure that even in newer Node environments that include
 * `fetch` by default, we still use the `node-fetch` polyfill and have access to the networking code
 */
import isomorphicFetch from 'isomorphic-fetch'
import { addCodyClientIdentificationHeaders } from './sourcegraph-api/client-name-version'
import type { BrowserOrNodeResponse } from './sourcegraph-api/graphql/client'

/**
 * In node environments, it might be necessary to set up a custom agent to control the network
 * requests being made.
 *
 * To do this, we have a mutable agent variable that can be set to an instance of `http.Agent` or
 * `https.Agent` (depending on the protocol of the URL) but that will be kept undefined for web
 * environments.
 *
 * Agent is a mutable ref so that we can override it from `fetch.node.ts`
 */
export const agent: { current: ((url: URL) => Agent) | undefined } = { current: undefined }

export function fetch(input: RequestInfo | URL, init?: RequestInit): Promise<BrowserOrNodeResponse> {
    init = init ?? {}
    const headers = new Headers(init?.headers)
    addCodyClientIdentificationHeaders(headers)
    init.headers = headers

    const initWithAgent: RequestInit & { agent: (typeof agent)['current'] } = {
        ...init,
        agent: agent.current,
    }
    return isomorphicFetch(input, initWithAgent)
}
