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