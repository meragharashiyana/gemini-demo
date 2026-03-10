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
      .then(response => {
        // 1. Check for HTTP errors
        if (!response.ok) {
          // This will trigger the .catch() block
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
        // 2. If the response is OK, proceed to get the text
        return response.text();
      })
      .then(message => {
        setDbMessage(message);
      })
      .catch(error => {
        // 3. This block catches both network errors and the error we threw above
        console.error('There was a problem with the fetch operation:', error);
        setDbMessage('Failed to load greeting.'); // Update UI to show an error
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
