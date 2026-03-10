import React, { useState, useEffect } from 'react';
import './App.css';

function App() {
  const [message, setMessage] = useState('');
  const [dbMessage, setDbMessage] = useState('');

  useEffect(() => {
    fetch('/api/hello')
      .then(response => response.text())
      .then(message => {
        setMessage(message);
      });
  }, []);

  const fetchDbGreeting = () => {
    fetch('/api/db-hello')
      .then(response => response.text())
      .then(message => {
        setDbMessage(message);
      });
  };

  return (
    <div className="App">
      <header className="App-header">
        <p>
          {message}
        </p>
        <button onClick={fetchDbGreeting}>Get Greeting from DB</button>
        {dbMessage && <p>Database Greeting: {dbMessage}</p>}
      </header>
    </div>
  );
}

export default App;
