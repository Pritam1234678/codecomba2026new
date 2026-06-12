import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

import { cloudflare } from "@cloudflare/vite-plugin";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), cloudflare()],
  server: {
    proxy: {
      // SSE stream endpoint — needs special handling to disable buffering
      '/api/submissions/stream': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        // Disable response buffering so SSE events flow through immediately
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            // Force chunked transfer — prevents Vite from buffering SSE
            proxyRes.headers['cache-control'] = 'no-cache';
            proxyRes.headers['x-accel-buffering'] = 'no';
          });
        },
      },
      // WebSocket for interactive compiler
      '/api/compiler/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
      // WebSocket for proctoring channel
      '/api/proctoring/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  },
  build: {
    target: 'es2020',
    minify: 'esbuild',
    cssMinify: 'esbuild',
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-react':  ['react', 'react-dom', 'react-router-dom'],
          'vendor-motion': ['framer-motion'],
          'vendor-monaco': ['@monaco-editor/react'],
          'vendor-xterm':  ['@xterm/xterm', '@xterm/addon-fit'],
          'vendor-utils':  ['axios', 'clsx', 'tailwind-merge'],
        }
      }
    },
    chunkSizeWarningLimit: 600,
  }
})