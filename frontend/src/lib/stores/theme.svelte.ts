import { browser } from '$app/environment';

function createTheme() {
    let dark = $state(false);

    function init() {
        if (!browser) return;
        const saved = localStorage.getItem('darkMode');
        dark = saved !== null ? saved === '1' : window.matchMedia('(prefers-color-scheme: dark)').matches;
    }

    function toggle() {
        dark = !dark;
    }

    return {
        get dark() {
            return dark;
        },
        init,
        toggle
    };
}

export const theme = createTheme();