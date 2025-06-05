// playlist-confirmation.js - Audio preview + navigation functionality
let currentAudio = null;
let currentButton = null;

function togglePreview(button, previewUrl) {
    if (!previewUrl || previewUrl === 'null') {
        return;
    }

    // If there's already an audio playing, stop it
    if (currentAudio && !currentAudio.paused) {
        currentAudio.pause();
        currentAudio.currentTime = 0;
        if (currentButton) {
            currentButton.innerHTML = '▶';
            currentButton.classList.remove('playing');
        }
    }

    // If clicking the same button that was playing, just stop
    if (currentButton === button) {
        currentAudio = null;
        currentButton = null;
        return;
    }

    // Find the audio element associated with this button
    const trackItem = button.closest('.track-item');
    const audio = trackItem.querySelector('audio');

    if (!audio) {
        console.error('No audio element found for this track');
        return;
    }

    // Set loading state
    button.innerHTML = '⏳';
    button.classList.add('loading');

    // Set up audio event listeners
    audio.addEventListener('loadstart', function() {
        button.innerHTML = '⏳';
        button.classList.add('loading');
    });

    audio.addEventListener('canplay', function() {
        button.innerHTML = '⏸';
        button.classList.remove('loading');
        button.classList.add('playing');
    });

    audio.addEventListener('ended', function() {
        button.innerHTML = '▶';
        button.classList.remove('playing');
        currentAudio = null;
        currentButton = null;
    });

    audio.addEventListener('error', function() {
        button.innerHTML = '❌';
        button.classList.remove('loading');
        setTimeout(() => {
            button.innerHTML = '▶';
        }, 2000);
    });

    // Play the audio
    audio.play().catch(function(error) {
        console.error('Error playing audio:', error);
        button.innerHTML = '❌';
        button.classList.remove('loading');
        setTimeout(() => {
            button.innerHTML = '▶';
        }, 2000);
    });

    currentAudio = audio;
    currentButton = button;
}

// Pause all audio when page is hidden (user switches tabs)
document.addEventListener('visibilitychange', function() {
    if (document.hidden && currentAudio && !currentAudio.paused) {
        currentAudio.pause();
        if (currentButton) {
            currentButton.innerHTML = '▶';
            currentButton.classList.remove('playing');
        }
    }
});

// Navigation and spinner functionality
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM fully loaded and parsed');

    // Add click event listener for the "Generate Another Playlist" button
    const generatePlaylistButton = document.querySelector('a[href="/api/generate-playlist"]');
    if (generatePlaylistButton) {
        console.log('Found generate playlist button');
        generatePlaylistButton.addEventListener('click', function(e) {
            console.log('Generate playlist button clicked');
            e.preventDefault();
            if (window.SpinnerUtils) {
                console.log('Using SpinnerUtils');
                window.SpinnerUtils.showAndRedirect('/api/generate-playlist');
            } else {
                console.log('Using fallback spinner');
                const loadingOverlay = document.getElementById('loading-overlay');
                if (loadingOverlay) {
                    loadingOverlay.style.display = 'flex';
                }
                setTimeout(() => {
                    window.location.href = '/api/generate-playlist';
                }, 50);
            }
        });
    } else {
        console.log('Generate playlist button not found');
    }

    // Handle refresh playlist form
    const refreshPlaylistForm = document.getElementById('refreshPlaylistForm');
    if (refreshPlaylistForm) {
        console.log('Found refresh playlist form');

        // Add submit event listener
        refreshPlaylistForm.addEventListener('submit', function(e) {
            console.log('Refresh playlist form submitted');
            e.preventDefault();

            // Get the artists value
            const artistsInput = refreshPlaylistForm.querySelector('input[name="artists"]');
            const artistsValue = artistsInput ? artistsInput.value : '';
            console.log('Artists value:', artistsValue);

            // Show spinner
            if (window.SpinnerUtils) {
                console.log('Using SpinnerUtils for redirect');
                window.SpinnerUtils.showAndRedirect('/api/playlist-generation?artists=' + encodeURIComponent(artistsValue));
            } else {
                console.log('Using fallback spinner for redirect');
                const loadingOverlay = document.getElementById('loading-overlay');
                if (loadingOverlay) {
                    loadingOverlay.style.display = 'flex';
                }
                setTimeout(() => {
                    window.location.href = '/api/playlist-generation?artists=' + encodeURIComponent(artistsValue);
                }, 50);
            }
        });

        // Also add click event listener to the button for redundancy
        const refreshButton = refreshPlaylistForm.querySelector('button[type="submit"]');
        if (refreshButton) {
            console.log('Found refresh button');
            refreshButton.addEventListener('click', function() {
                console.log('Refresh button clicked');
            });
        }
    } else {
        console.log('Refresh playlist form not found');
    }

    // Check if SpinnerUtils is available
    console.log('SpinnerUtils available:', !!window.SpinnerUtils);

    // Check if loading overlay exists
    console.log('Loading overlay exists:', !!document.getElementById('loading-overlay'));
});