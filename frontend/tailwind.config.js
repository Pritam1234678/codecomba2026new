/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Legacy colors (kept for backward compat)
        'code-green': '#00ff41',
        'code-black': '#131313',
        'code-dark': '#201f1f',
        'code-gray': '#252323',

        // Design system colors
        'primary': '#7a4a4a',
        'primary-container': '#5f3334',
        'on-primary': '#ffffff',
        'on-primary-container': '#fdbdbc',

        'secondary': '#4a6a8a',
        'secondary-container': '#badaff',
        'on-secondary': '#ffffff',
        'on-secondary-container': '#40607f',

        'tertiary': '#a67c52',
        'tertiary-container': '#74502a',
        'on-tertiary': '#ffffff',
        'on-tertiary-container': '#f6c595',

        'background': '#131313',
        'surface': '#201f1f',
        'surface-variant': '#2a2828',
        'surface-container': '#252323',
        'surface-container-high': '#2e2c2c',
        'surface-container-low': '#1c1b1b',
        'surface-bright': '#2e2c2c',

        'on-background': '#e5e2e1',
        'on-surface': '#e5e2e1',
        'on-surface-variant': '#a09898',

        'outline': '#5a4f4f',
        'outline-variant': '#3a3232',

        'error': '#f2b8b8',
        'error-container': '#8c1d18',
        'on-error': '#601410',
        'on-error-container': '#f9dedc',
      },
      fontFamily: {
        mono: ['"JetBrains Mono"', 'monospace'],
        sans: ['Inter', 'sans-serif'],
        code: ['"JetBrains Mono"', 'monospace'],
      },
      borderRadius: {
        DEFAULT: '0.25rem',
        lg: '0.5rem',
        xl: '0.75rem',
        '2xl': '1rem',
        '3xl': '1.5rem',
        full: '9999px',
      },
      boxShadow: {
        'diffused': '0 4px 20px rgba(0, 0, 0, 0.4)',
        'primary': '0 4px 20px rgba(122, 74, 74, 0.25)',
        'bronze': '0 4px 20px rgba(166, 124, 82, 0.2)',
        'secondary': '0 4px 20px rgba(74, 106, 138, 0.2)',
      },
    },
  },
  plugins: [],
}
