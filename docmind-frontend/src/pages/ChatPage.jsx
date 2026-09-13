import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../AuthContext';
import { api } from '../api';

export default function ChatPage() {
  const { auth } = useAuth();
  const [sessionId, setSessionId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const bottomRef = useRef(null);

  useEffect(() => {
    api.createSession(auth.token)
      .then((session) => setSessionId(session.id))
      .catch((err) => setError(err.message));
  }, [auth.token]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!question.trim() || !sessionId || sending) return;

    const asked = question.trim();
    setMessages((prev) => [...prev, { role: 'USER', content: asked }]);
    setQuestion('');
    setSending(true);
    setError(null);

    try {
      const response = await api.query(auth.token, sessionId, asked);
      setMessages((prev) => [...prev, { role: 'ASSISTANT', content: response.answer, sources: response.sources }]);
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="chat-page">
      <div className="chat-log">
        {messages.length === 0 && (
          <div className="chat-empty">
            Ask a question about the documents that have been uploaded.
            {!sessionId && !error && <div className="chat-empty-hint">Starting a new session…</div>}
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} className={`chat-bubble ${m.role === 'USER' ? 'chat-bubble-user' : 'chat-bubble-assistant'}`}>
            <div className="chat-bubble-content">{m.content}</div>
            {m.sources && m.sources.length > 0 && (
              <div className="chat-sources">
                {m.sources.map((s, j) => (
                  <span key={j} className="chat-source-chip">
                    {s.filename}{s.page != null ? `, p.${s.page}` : ''}
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}
        {sending && <div className="chat-bubble chat-bubble-assistant chat-bubble-pending">Thinking…</div>}
        <div ref={bottomRef} />
      </div>

      {error && <div className="error-banner">{error}</div>}

      <form className="chat-input-row" onSubmit={handleSubmit}>
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask a question…"
          disabled={!sessionId}
        />
        <button type="submit" disabled={!sessionId || sending || !question.trim()}>Send</button>
      </form>
    </div>
  );
}
