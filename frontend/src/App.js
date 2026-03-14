import React, { useState, useEffect } from 'react';
import './App.css';

function App() {
  const [message, setMessage] = useState('');
  const [dbMessage, setDbMessage] = useState('');
  const [cachedDbMessage, setCachedDbMessage] = useState('');
  const [hybridDbMessage, setHybridDbMessage] = useState('');
  const [cacheStatus, setCacheStatus] = useState('');
  const [cacheInspect, setCacheInspect] = useState(null);
  const [dbResponseTime, setDbResponseTime] = useState(null);
  const [cachedResponseTime, setCachedResponseTime] = useState(null);
  const [hybridResponseTime, setHybridResponseTime] = useState(null);

  useEffect(() => {
    fetch('/api/hello')
      .then(response => response.text())
      .then(message => {
        setMessage(message);
      });
  }, []);

  const fetchDbGreeting = () => {
    const start = performance.now();
    fetch('/api/db-hello')
      .then(response => {
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        return response.text();
      })
      .then(message => {
        setDbMessage(message);
        setDbResponseTime(Math.round(performance.now() - start));
      })
      .catch(error => {
        console.error('There was a problem with fetch:', error);
        setDbMessage('Failed to load greeting.');
        setDbResponseTime(null);
      });
  };

  const fetchCachedDbGreeting = () => {
    const start = performance.now();
    fetch('/api/cached-db-hello')
      .then(response => {
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        return response.text();
      })
      .then(message => {
        setCachedDbMessage(message);
        setCachedResponseTime(Math.round(performance.now() - start));
      })
      .catch(error => {
        console.error('Failed to fetch cached greeting:', error);
        setCachedDbMessage('Failed to load cached greeting.');
        setCachedResponseTime(null);
      });
  };

  const fetchHybridCachedDbGreeting = () => {
    const start = performance.now();
    fetch('/api/hybrid-cached-db-hello')
      .then(response => {
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        return response.text();
      })
      .then(message => {
        setHybridDbMessage(message);
        setHybridResponseTime(Math.round(performance.now() - start));
      })
      .catch(error => {
        console.error('Failed to fetch hybrid cached greeting:', error);
        setHybridDbMessage('Failed to load hybrid cached greeting.');
        setHybridResponseTime(null);
      });
  };

  const clearCache = () => {
    fetch('/api/cache-clear')
      .then(response => response.text())
      .then(message => setCacheStatus(message))
      .catch(() => setCacheStatus('Failed to clear cache.'));
  };

  const clearL1Cache = () => {
    fetch('/api/cache/l1-clear')
      .then(response => response.text())
      .then(message => setCacheStatus(message))
      .catch(() => setCacheStatus('Failed to clear L1 cache.'));
  };

  const inspectCache = () => {
    fetch('/api/cache/inspect')
      .then(response => response.json())
      .then(data => setCacheInspect(data))
      .catch(() => setCacheInspect({ error: 'Failed to inspect cache.' }));
  };

  return (
    <div className="App">
      <header className="App-header">
        <p>{message}</p>

        <div style={{ margin: '1rem 0', border: '1px solid rgba(255,255,255,0.2)', padding: '0.75rem', borderRadius: '8px' }}>
          <h3 style={{ margin: '0 0 0.5rem' }}>DB Greeting (No Cache)</h3>
          <button onClick={fetchDbGreeting}>Fetch from DB</button>
          {dbMessage && <p style={{ marginTop: '0.5rem' }}>DB Result: {dbMessage}</p>}
          {dbResponseTime !== null && <p style={{ marginTop: '0.2rem' }}>Response time: {dbResponseTime} ms</p>}
        </div>

        <div style={{ margin: '1rem 0', border: '1px solid rgba(255,255,255,0.2)', padding: '0.75rem', borderRadius: '8px' }}>
          <h3 style={{ margin: '0 0 0.5rem' }}>DB Greeting (With Cache)</h3>
          <button onClick={fetchCachedDbGreeting}>Fetch Cached Greeting</button>
          {cachedDbMessage && <p style={{ marginTop: '0.5rem' }}>Cached Result: {cachedDbMessage}</p>}
          {cachedResponseTime !== null && <p style={{ marginTop: '0.2rem' }}>Response time: {cachedResponseTime} ms</p>}
        </div>

        <div style={{ margin: '1rem 0', border: '1px solid rgba(255,255,255,0.2)', padding: '0.75rem', borderRadius: '8px' }}>
          <h3 style={{ margin: '0 0 0.5rem' }}>DB Greeting (Hybrid Cache L1+Caffeine + L2+Redis)</h3>
          <button onClick={fetchHybridCachedDbGreeting}>Fetch Hybrid Cached Greeting</button>
          {hybridDbMessage && <p style={{ marginTop: '0.5rem' }}>Hybrid Cached Result: {hybridDbMessage}</p>}
          {hybridResponseTime !== null && <p style={{ marginTop: '0.2rem' }}>Response time: {hybridResponseTime} ms</p>}
        </div>

        <div style={{ marginTop: '0.6rem' }}>
          <button onClick={clearCache} style={{ marginRight: '0.5rem' }}>Clear All Cache</button>
          <button onClick={clearL1Cache} style={{ marginRight: '0.5rem' }}>Clear L1 Cache (Caffeine)</button>
          <button onClick={inspectCache}>Inspect Cache</button>

          {cacheStatus && <p style={{ marginTop: '0.4rem' }}>{cacheStatus}</p>}
          {cacheInspect && (
            <div style={{ marginTop: '0.4rem', textAlign: 'left' }}>
              <div style={{ marginBottom: '0.25rem' }}>
                <strong>Redis enabled:</strong> {cacheInspect.redisEnabled ? 'Yes' : 'No'}
              </div>
              {!cacheInspect.redisEnabled && (
                <div style={{ marginBottom: '0.25rem', color: '#ffd700' }}>
                  Redis is not enabled or not reachable. Ensure Redis is running and the app was started with Redis enabled.
                </div>
              )}
              <pre style={{ background: 'rgba(0,0,0,0.2)', padding: '0.5rem', borderRadius: '6px' }}>
                {JSON.stringify(cacheInspect, null, 2)}
              </pre>
            </div>
          )}
        </div>
      </header>
    </div>
  );
}

export default App;
