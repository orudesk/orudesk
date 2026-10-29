import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// The package as published to npm (Day 61): plain JavaScript modules in dist/, one file per source file, with
// every dependency left as an import for the app's bundler to resolve. Types come from tsc (tsconfig.build.json).
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    sourcemap: true,
    minify: false, // the app minifies; readable code here makes stack traces and debugging easier
    lib: {
      entry: {
        index: 'src/index.ts',
        testing: 'src/testing.ts',
        'test-setup': 'src/test-setup.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      // React, Mantine, the router... are the app's: bundling a copy would mean two Reacts (Day 50)
      external: (id) => !id.startsWith('.') && !id.startsWith('/') && !id.startsWith('\0'),
      output: { preserveModules: true, preserveModulesRoot: 'src', entryFileNames: '[name].js' },
    },
    copyPublicDir: false,
  },
});
