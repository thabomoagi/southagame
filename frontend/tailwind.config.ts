import type { Config } from 'tailwindcss';

export default {
    content: ['./src/**/*.{html,js,svelte,ts}'],
    darkMode: 'class',
    theme: {
        extend: {
            colors: {
                springbok: '#007A5E',
                'springbok-bright': '#00A67D',
                'sa-yellow': '#FFC72C',
                'dark-bg': '#121212',
                'dark-surface': '#1E1E1E'
            },
            fontFamily: {
                sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif']
            },
            animation: {
                'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite'
            }
        }
    },
    plugins: []
} satisfies Config;