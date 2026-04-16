/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // MTG mana colour palette
        mana: {
          white:    '#F9FAF4',
          blue:     '#0E68AB',
          black:    '#150B00',
          red:      '#D3202A',
          green:    '#00733E',
          colorless:'#BFBFBF',
          gold:     '#D4AF37',
        },
        board: {
          felt:     '#1A3D1A',
          border:   '#2E5E2E',
          overlay:  'rgba(0,0,0,0.6)',
        },
      },
      fontFamily: {
        display: ['"Cinzel"', 'serif'],
        body:    ['"Inter"', 'sans-serif'],
        mono:    ['"JetBrains Mono"', 'monospace'],
      },
    },
  },
  plugins: [],
}
