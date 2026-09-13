const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function request(path, { method = 'GET', body, token, isForm = false } = {}) {
  const headers = {};
  if (!isForm) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: isForm ? body : body ? JSON.stringify(body) : undefined,
  });

  const text = await res.text();
  const data = text ? tryParseJson(text) : null;

  if (!res.ok) {
    const message = (data && data.message) || (typeof data === 'string' ? data : null) || res.statusText;
    throw new Error(message);
  }
  return data;
}

function tryParseJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

export const api = {
  register: (payload) => request('/api/auth/register', { method: 'POST', body: payload }),
  login: (payload) => request('/api/auth/login', { method: 'POST', body: payload }),

  createSession: (token) => request('/api/chat/sessions', { method: 'POST', token }),
  getMessages: (token, sessionId) => request(`/api/chat/sessions/${sessionId}/messages`, { token }),
  query: (token, sessionId, question) =>
    request('/api/chat/query', { method: 'POST', token, body: { sessionId, question } }),

  listDocuments: (token) => request('/api/documents', { token }),
  uploadDocument: (token, file) => {
    const form = new FormData();
    form.append('file', file);
    return request('/api/documents/upload', { method: 'POST', token, body: form, isForm: true });
  },
};
