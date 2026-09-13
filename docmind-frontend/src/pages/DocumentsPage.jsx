import { useEffect, useState } from 'react';
import { useAuth } from '../AuthContext';
import { api } from '../api';

export default function DocumentsPage() {
  const { auth } = useAuth();
  const [documents, setDocuments] = useState([]);
  const [error, setError] = useState(null);
  const [uploading, setUploading] = useState(false);
  const canUpload = auth.role === 'ADMIN' || auth.role === 'STAFF';

  function loadDocuments() {
    api.listDocuments(auth.token).then(setDocuments).catch((err) => setError(err.message));
  }

  useEffect(loadDocuments, [auth.token]);

  async function handleFileChange(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';
    setUploading(true);
    setError(null);
    try {
      await api.uploadDocument(auth.token, file);
      loadDocuments();
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="documents-page">
      <div className="documents-header">
        <h2>Documents</h2>
        {canUpload && (
          <label className="upload-button">
            {uploading ? 'Uploading…' : 'Upload PDF'}
            <input type="file" accept="application/pdf" onChange={handleFileChange} disabled={uploading} hidden />
          </label>
        )}
      </div>
      {error && <div className="error-banner">{error}</div>}
      <table className="documents-table">
        <thead>
          <tr><th>Filename</th><th>Status</th><th>Uploaded</th></tr>
        </thead>
        <tbody>
          {documents.map((d) => (
            <tr key={d.id}>
              <td>{d.filename}</td>
              <td><span className={`status-pill status-${d.status?.toLowerCase()}`}>{d.status}</span></td>
              <td>{d.uploadedAt ? new Date(d.uploadedAt).toLocaleString() : '—'}</td>
            </tr>
          ))}
          {documents.length === 0 && (
            <tr><td colSpan={3} className="documents-empty">No documents uploaded yet.</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
