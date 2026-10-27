import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import type { OrudeskModule } from './module';
import { createServices } from './services';

/** Everything an edition's main.tsx needs: `startOrudeskApp([coreModule, ngoModule])`. */
export function startOrudeskApp(modules: OrudeskModule[], root = document.getElementById('root')) {
  if (!root) throw new Error('index.html has no #root element');
  createRoot(root).render(
    <StrictMode>
      <App services={createServices()} modules={modules} />
    </StrictMode>,
  );
}
