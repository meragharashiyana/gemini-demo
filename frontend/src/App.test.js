import { render, screen } from '@testing-library/react';
import App from './App';

test('renders caching UI buttons', () => {
  render(<App />);
  const dbButton = screen.getByText(/Fetch from DB/i);
  const cachedButton = screen.getByText(/Fetch Cached Greeting/i);
  const hybridButton = screen.getByText(/Fetch Hybrid Cached Greeting/i);
  expect(dbButton).toBeInTheDocument();
  expect(cachedButton).toBeInTheDocument();
  expect(hybridButton).toBeInTheDocument();
});
