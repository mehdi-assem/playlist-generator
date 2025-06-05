// generate-playlist.js - Only for the playlist generation page
document.addEventListener('DOMContentLoaded', function() {

// Make sure we're on the right page
const playlistForm = document.getElementById('playlistForm');
if (!playlistForm) {
    return; // Exit if we're not on the generate playlist page
}

function selectMode(mode) {
    // Update radio buttons
    const inspiredRadio = document.getElementById('inspired');
    const customRadio = document.getElementById('custom');
    const freeformRadio = document.getElementById('freeform');

    if (inspiredRadio && customRadio && freeformRadio) {
        inspiredRadio.checked = mode === 'inspired';
        customRadio.checked = mode === 'custom';
        freeformRadio.checked = mode === 'freeform';
    }

    // Show/hide customization options
    const customOptions = document.getElementById('customization-options');
    const freeformOptions = document.getElementById('freeform-options');

    if (customOptions && freeformOptions) {
        if (mode === 'custom') {
            customOptions.style.display = 'block';
            customOptions.style.animation = 'slideDown 0.3s ease';
            freeformOptions.style.display = 'none';
        } else if (mode === 'freeform') {
            freeformOptions.style.display = 'block';
            freeformOptions.style.animation = 'slideDown 0.3s ease';
            customOptions.style.display = 'none';
        } else {
            customOptions.style.display = 'none';
            freeformOptions.style.display = 'none';
        }
    }
}

function toggleSection(sectionName) {
    const content = document.getElementById(sectionName + '-content');
    const toggle = document.getElementById(sectionName + '-toggle');

    if (content && toggle) {
        if (content.classList.contains('expanded')) {
            content.classList.remove('expanded');
            toggle.classList.remove('expanded');
        } else {
            content.classList.add('expanded');
            toggle.classList.add('expanded');
        }
    }
}

function selectMood(element, mood) {
    // Remove selection from all mood buttons
    const moodButtons = document.querySelectorAll('.mood-button');
    if (moodButtons) {
        moodButtons.forEach(btn => {
            btn.classList.remove('selected');
        });
    }

    // Add selection to clicked button
    if (element) {
        element.classList.add('selected');
    }
    const selectedMoodInput = document.getElementById('selected-mood');
    if (selectedMoodInput) {
        selectedMoodInput.value = mood;
    }
}

function quickGenerate(type) {
    // Set mode to inspired
    selectMode('inspired');

    // Add quick generation type
    if (playlistForm) {
        let input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'quickType';
        input.value = type;
        playlistForm.appendChild(input);

        // Submit form
        playlistForm.submit();
    }
}

// Auto-expand first section when custom mode is selected
const customRadio = document.getElementById('custom');
if (customRadio) {
    customRadio.addEventListener('change', function() {
        if (this.checked) {
            setTimeout(() => {
                toggleSection('mood');
            }, 300);
        }
    });
}

// Form submission logic - show spinner when form is submitted
if (playlistForm) {
    playlistForm.addEventListener('submit', function(e) {
        // Show spinner using shared utility
        if (window.SpinnerUtils) {
            window.SpinnerUtils.show();
        }

        // Disable generate button
        const generateBtn = this.querySelector('.generate-btn');
        if (generateBtn) {
            generateBtn.disabled = true;
        }
    });
}

// Toggle listening history timeframe based on checkbox
const checkbox = document.getElementById('use-listening-history');
const timeframe = document.getElementById('history-timeframe');

if (checkbox && timeframe) {
    checkbox.addEventListener('change', function() {
        timeframe.style.display = this.checked ? 'block' : 'none';
    });
}

// Make functions globally available for onclick handlers in HTML
window.selectMode = selectMode;
window.toggleSection = toggleSection;
window.selectMood = selectMood;
window.quickGenerate = quickGenerate;
});