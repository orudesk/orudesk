import { defineConfig } from 'vite';

// The package as published to npm (Day 61): see web-core's vite.lib.config.ts.
export default defineConfig({
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    sourcemap: true,
    minify: false,
    lib: { entry: { index: 'src/index.ts' }, formats: ['es'] },
    rollupOptions: {
      external: (id) => !id.startsWith('.') && !id.startsWith('/') && !id.startsWith('\0'),
      output: { entryFileNames: '[name].js' },
    },
    copyPublicDir: false,
  },
});
