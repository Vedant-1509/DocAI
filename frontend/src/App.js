import React, { useState } from 'react';
import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import Login from './components/Login';
import Signup from './components/Signup';
import ChatInterface from './components/ChatInterface'; // this is the DocAI chat UI

function App() {
  const [user, setUser] = useState(null);

  return (
    <Router>
      <Routes>
        <Route path="/" element={user ? <Navigate to="/chat" /> : <Login onLogin={setUser} />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/chat" element={user ? <ChatInterface user={user} /> : <Navigate to="/" />} />
      </Routes>
    </Router>
  );
}

export default App;
