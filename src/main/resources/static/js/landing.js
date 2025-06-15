// Configuration
const config = {
    apiBaseUrl: window.config?.apiBaseUrl || 'http://localhost:8080/api',
    redirectServerIp: window.config?.redirectServerIp || 'http://localhost:8000'
};

// DOM Elements
const elements = {
    loginButton: document.getElementById('spotifyLoginBtn')
};

// Initialize the application
function init() {
    setupEventListeners();
    addAnimationToTitle();
}

// Set up event listeners
function setupEventListeners() {
    if (elements.loginButton) {
        elements.loginButton.addEventListener('click', handleSpotifyLogin);
    } else {
        console.error('Login button not found');
    }
}

// Handle Spotify login
async function handleSpotifyLogin() {
    try {
        showLoadingState();
        const loginUrl = await fetchSpotifyLoginUrl();
        redirectToSpotify(loginUrl);
    } catch (error) {
        handleLoginError(error);
    }
}

// Fetch Spotify login URL from backend
async function fetchSpotifyLoginUrl() {
    const response = await fetch(`${config.apiBaseUrl}/login`);

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.text();
}

// Redirect to Spotify login page
function redirectToSpotify(url) {
    window.location.href = url;
}

// Show loading state on the button
function showLoadingState() {
    if (elements.loginButton) {
        elements.loginButton.disabled = true;
        elements.loginButton.innerHTML = `
            <span class="spinner"></span>
            Loading...
        `;
    }
}

// Handle login errors
function handleLoginError(error) {
    console.error('Login error:', error);
    if (elements.loginButton) {
        elements.loginButton.disabled = false;
        elements.loginButton.innerHTML = `
            <svg class="spotify-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="12" cy="12" r="12" fill="#1DB954"/>
                <path d="M8 11.9947C8 9.78449 8 8.87375 8.8655 8.40575C9.79525 7.89125 10.994 7.94575 12 8.54375C13.006 7.94575 14.2047 7.89125 15.1345 8.40575C16 8.87375 16 9.78449 16 11.9947C16 15.9607 16 18.007 12 18.007C8 18.007 8 15.9607 8 11.9947Z" fill="white"/>
                <path d="M12.865 10.001C13.2055 10.001 13.5005 10.3515 13.5005 10.7265C13.5005 11.1015 13.2055 11.452 12.865 11.452V10.001Z" fill="white"/>
                <path d="M11.131 10.001C11.4715 10.001 11.7665 10.3515 11.7665 10.7265C11.7665 11.1015 11.4715 11.452 11.131 11.452V10.001Z" fill="white"/>
                <path d="M10.3975 12.001C10.738 12.001 11.033 12.3515 11.033 12.7265C11.033 13.1015 10.738 13.452 10.3975 13.452V12.001Z" fill="white"/>
            </svg>
            Continue with Spotify
        `;
    }

    // In a real app, you might show an error message to the user
    alert('Failed to initiate Spotify login. Please try again.');
}

// Add animation to the title
function addAnimationToTitle() {
    const title = document.querySelector('.landing-title');
    if (title) {
        title.style.opacity = '0';
        title.style.transform = 'translateY(20px)';

        setTimeout(() => {
            title.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
            title.style.opacity = '1';
            title.style.transform = 'translateY(0)';
        }, 100);
    }
}

// Initialize the app when DOM is loaded
document.addEventListener('DOMContentLoaded', init);
