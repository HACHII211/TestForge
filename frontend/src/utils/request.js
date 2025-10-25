import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
    baseURL: 'http://localhost:9090',
    timeout: 30000,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json'
    }
});

function getStoredToken() {
    return localStorage.getItem('auth.token') || localStorage.getItem('jwt') || null;
}
function saveTokenFromHeader(headerToken) {
    if (!headerToken) return;
    const normalized = headerToken.startsWith('Bearer ') ? headerToken.substring(7) : headerToken;
    localStorage.setItem('auth.token', normalized);
}
function toAuthHeader(token) {
    if (!token) return undefined;
    return `Bearer ${token}`;
}

request.interceptors.request.use(
    config => {
        const token = getStoredToken();
        if (token) {
            config.headers['Authorization'] = toAuthHeader(token);
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

export const loadImage = (imageUrl) => {
    const token = getStoredToken();
    const headers = {};
    if (token) headers['Authorization'] = toAuthHeader(token);
    return request.get(imageUrl, {
        headers,
        responseType: 'blob'
    });
};

request.interceptors.response.use(
    response => {
        let res = response.data;

        if (typeof res === 'string') {
            try {
                res = res ? JSON.parse(res) : res;
            } catch (e) {
            }
        }

        const returnedAuth = response.headers && (response.headers['authorization'] || response.headers['Authorization']);
        if (returnedAuth) {
            saveTokenFromHeader(returnedAuth);
        }

        return res;
    },
    error => {
        const { response } = error;

        if (!response) {
            ElMessage.error('无法连接到服务器');
            return Promise.reject(error);
        }

        const { status, data } = response;

        if (status === 401) {
            ElMessage.error('登录状态失效，请重新登录');
            localStorage.removeItem('auth.token'); // 清除本地 token
            window.location.href = '/login'; // 跳转到登录页
        } else if (status === 404) {
            ElMessage.error('接口不存在');
        } else if (status === 500) {
            ElMessage.error('服务器错误');
        } else {
            ElMessage.error(data?.message || '请求异常');
        }

        return Promise.reject(error);
    }
);

/**
 * Robust streaming POST helper (SSE / JSONL / plain chunk)
 *
 * Usage:
 *   const controller = askStream(messages, {
 *     url: '/qa/askStream', // default (relative to axios baseURL)
 *     onMessage: (chunk) => { ... },    // called for each chunk (string)
 *     onDone: () => { ... },            // called when stream ends normally
 *     onError: (err) => { ... }         // called on error
 *   });
 *
 *   // to abort:
 *   controller.abort();
 */
export function askStream(payload, { url = '/qa/askStream', onMessage, onDone, onError } = {}) {
    const controller = new AbortController();
    const signal = controller.signal;

    // Build absolute URL (if url is relative, prefix with axios baseURL)
    let finalUrl = url;
    try {
        // if url is relative (doesn't start with http), prefix with axios baseURL
        if (!/^https?:\/\//i.test(url)) {
            const base = (request && request.defaults && request.defaults.baseURL) ? request.defaults.baseURL.replace(/\/$/, '') : '';
            finalUrl = base + (url.startsWith('/') ? url : '/' + url);
        }
    } catch (e) {
        finalUrl = url;
    }

    (async () => {
        try {
            // build headers (include Authorization if present)
            const headers = { 'Content-Type': 'application/json' };
            const token = getStoredToken();
            if (token) headers['Authorization'] = toAuthHeader(token);

            const res = await fetch(finalUrl, {
                method: 'POST',
                headers,
                body: JSON.stringify(payload),
                signal
            });

            if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);

            if (!res.body) {
                // no streaming body available — fallback: read as text once
                const text = await res.text();
                // try parse as JSON array/obj or plain text
                try {
                    const parsed = JSON.parse(text);
                    // if parsed is array/object, extract text content heuristically
                    if (Array.isArray(parsed)) {
                        parsed.forEach(p => onMessage?.(String(p)));
                    } else {
                        const txt = parsed?.delta || parsed?.text || parsed?.content || parsed?.message || JSON.stringify(parsed);
                        onMessage?.(String(txt));
                    }
                } catch {
                    onMessage?.(text);
                }
                onDone?.();
                return;
            }

            const reader = res.body.getReader();
            const decoder = new TextDecoder('utf-8');

            // For SSE-like parsing we collect lines into an event buffer until blank line
            let eventBuffer = '';
            // For plain text (no newline) we will forward immediate chunks
            while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                const chunk = decoder.decode(value, { stream: true });
                if (!chunk) continue;

                // If chunk contains newline(s), treat as line-based stream (SSE/JSONL)
                if (chunk.includes('\n')) {
                    // split by lines, process each
                    const lines = chunk.split(/\r?\n/);
                    for (let line of lines) {
                        // if line is empty, event boundary -> process eventBuffer
                        if (line.trim() === '') {
                            if (eventBuffer.trim()) {
                                processEvent(eventBuffer.trim(), { onMessage, onDone, onError, controller });
                            }
                            eventBuffer = '';
                            continue;
                        }
                        // append line to eventBuffer (preserve newlines inside data:)
                        eventBuffer += (eventBuffer ? '\n' : '') + line;
                    }
                    // keep eventBuffer for next chunk (may be partial)
                } else {
                    // no newline in chunk -> likely a raw chunkable text, forward immediately
                    // but still try to trim common SSE prefix
                    const possible = chunk.trim();
                    if (!possible) continue;
                    // if chunk looks like 'data: ...' handle it as event
                    if (/^\s*data:\s*/.test(possible)) {
                        // treat as whole event
                        const cleaned = possible.split(/\r?\n/).map(l => l.replace(/^data:\s*/, '')).join('\n');
                        processEvent(cleaned, { onMessage, onDone, onError, controller });
                    } else {
                        // direct text chunk - forward as-is
                        onMessage?.(chunk);
                    }
                }
            }

            // process leftover eventBuffer
            if (eventBuffer && eventBuffer.trim()) {
                processEvent(eventBuffer.trim(), { onMessage, onDone, onError, controller });
            }

            onDone?.();
        } catch (err) {
            if (err.name === 'AbortError') {
                onError?.({ message: 'aborted' });
            } else {
                onError?.(err);
            }
        }
    })();

    return controller;
}

/**
 * Helper to process a single event payload string.
 * Recognizes SSE-style 'data:' lines, JSON objects, plain text, and [DONE] markers.
 */
function processEvent(raw, { onMessage, onDone, onError, controller }) {
    if (!raw) return;

    // If the event contains "data:" prefixes (SSE multi-line), strip them
    let payload = raw;
    if (/^data:\s*/i.test(payload)) {
        payload = payload.split(/\r?\n/).map(l => l.replace(/^data:\s*/i, '')).join('\n');
    }

    // Trim
    payload = payload.trim();

    // End markers
    if (payload === '[DONE]' || payload === 'data: [DONE]' || payload === '__DONE__' || payload === 'DONE') {
        onDone?.();
        return;
    }

    // Try JSON parse
    try {
        const obj = JSON.parse(payload);
        // Handle various provider shapes:
        // - OpenAI-like: { choices: [{ delta: { content: "..." } }] }
        // - other: { delta: "..."} or { content: "..." } or { text: "..." }
        let text = null;

        if (obj) {
            if (obj.choices && Array.isArray(obj.choices)) {
                // try find incremental delta content
                for (const ch of obj.choices) {
                    if (ch.delta && (ch.delta.content || ch.delta)) {
                        text = (ch.delta.content || ch.delta || ch.delta?.content || '').toString();
                        break;
                    }
                    if (ch.text) {
                        text = ch.text.toString();
                        break;
                    }
                }
            } else if (typeof obj.delta === 'string') {
                text = obj.delta;
            } else if (obj.delta && typeof obj.delta === 'object' && obj.delta.content) {
                text = obj.delta.content;
            } else if (obj.content) {
                text = obj.content;
            } else if (obj.text) {
                text = obj.text;
            } else {
                // fallback to stringify
                text = JSON.stringify(obj);
            }
        }

        if (text != null) {
            onMessage?.(String(text));
            return;
        }
    } catch (e) {
        // not JSON — continue to treat as plain text
    }

    // fallback plain text
    onMessage?.(payload);
}

export default request;
