/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: '#1D9E75',
        secondary: '#2C3E50',
        dark: '#1E1E1E',
        darker: '#141414',
        card: '#2A2A2A',
      },
    },
  },
  plugins: [],
};