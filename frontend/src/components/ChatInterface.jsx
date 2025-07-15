import React, { useState } from 'react';
import axios from 'axios';
import "./ChatInterface.css"; // Assuming you have some styles for the chat interface

export default function ChatInterface({ user }) {
  const [mode, setMode] = useState(''); // 'upload' | 'crawl' | 'stored'
  const [question, setQuestion] = useState('');
  const [url, setUrl] = useState('');
  const [file, setFile] = useState(null);
  const [answer, setAnswer] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setAnswer('');

    try {
      let res;
      if (mode === 'stored') {
        res = await axios.post('http://localhost:8080/api/ask', null, {
          params: { userId: user.userId, question }
        });
      } else if (mode === 'crawl') {
        res = await axios.post('http://localhost:8080/api/crawl-and-ask', null, {
          params: { url, userId: user.userId, question }
        });
      } else if (mode === 'upload') {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('question', question);
        formData.append('userId', user.userId);

        res = await axios.post('http://localhost:8080/api/ask-from-file', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
      }

      if (res?.data?.success) {
        setAnswer(res.data.data);
      } else {
        setAnswer(`Error: ${res?.data?.message || 'Unknown error'}`);
      }
    } catch (err) {
      setAnswer(`Error: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const renderForm = () => {
    if (!mode) return null;

    return (
      <form onSubmit={handleSubmit} style={{ marginTop: 20 }}>
        {mode === 'crawl' && (
          <input
            type="text"
            placeholder="Enter website URL"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            required
          />
        )}
        {mode === 'upload' && (
          <input
            type="file"
            accept=".pdf"
            onChange={(e) => setFile(e.target.files[0])}
            required
          />
        )}
        <input
          type="text"
          placeholder="Ask your question..."
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Asking...' : 'Ask'}
        </button>
      </form>
    );
  };

  return (
    <div style={{ padding: '20px', maxWidth: '600px', margin: 'auto' }}>
      <h2>Welcome, {user.name}</h2>
      <p>Select how you want to ask your question:</p>
      <div style={{ marginBottom: 15 }}>
        <button onClick={() => setMode('upload')}>📄 Upload Document</button>
        <button onClick={() => setMode('crawl')}>🌐 Crawl Website</button>
        <button onClick={() => setMode('stored')}>💬 Ask from Stored Docs</button>
      </div>

      {renderForm()}

      {answer && (
        <div style={{ marginTop: 20 }}>
          <strong>Answer:</strong>
          <p>{answer}</p>
        </div>
      )}
    </div>
  );
}
