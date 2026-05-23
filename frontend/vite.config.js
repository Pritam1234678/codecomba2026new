import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
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
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  },
  build: {
    // Split vendor chunks so browsers can cache them independently
    rollupOptions: {
      output: {
        manualChunks: {
          // React core — changes rarely, long-lived cache
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          // Animation libs — large but stable
          'vendor-motion': ['framer-motion', 'gsap'],
          // 3D libs — only used on 404 page, lazy-loaded
          'vendor-three': ['three', '@react-three/fiber', '@react-three/drei'],
          // Monaco editor — very large, separate chunk
          'vendor-monaco': ['@monaco-editor/react'],
          // Axios + utilities
          'vendor-utils': ['axios', 'clsx', 'tailwind-merge'],
        }
      }
    },
    // Raise warning threshold — we know about the chunks
    chunkSizeWarningLimit: 600,
  }
})
