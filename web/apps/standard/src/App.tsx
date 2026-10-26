import { useApiHealth } from './useApiHealth';

const messages = {
  checking: 'Checking the API...',
  up: 'The API is up.',
  down: 'The API is not reachable. Is it running on port 8080?',
} as const;

export function App() {
  const health = useApiHealth();
  return (
    <main>
      <h1>Orudesk</h1>
      <p role="status">{messages[health]}</p>
    </main>
  );
}
