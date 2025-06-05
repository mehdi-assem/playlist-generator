// spinner-utils.js - Shared spinner functionality
window.SpinnerUtils = {
    show: function() {
        const loadingOverlay = document.getElementById('loading-overlay');
        if (loadingOverlay) {
            loadingOverlay.style.display = 'flex';
        }
    },

    hide: function() {
        const loadingOverlay = document.getElementById('loading-overlay');
        if (loadingOverlay) {
            loadingOverlay.style.display = 'none';
        }
    },

    // Show spinner and redirect after a short delay
    showAndRedirect: function(url, delay = 50) {
        this.show();
        setTimeout(() => {
            window.location.href = url;
        }, delay);
    },

    // Auto-hide spinner when page loads
    initAutoHide: function() {
        window.addEventListener('load', () => {
            this.hide();
        });
    }
};

// Auto-initialize
document.addEventListener('DOMContentLoaded', function() {
    window.SpinnerUtils.initAutoHide();
});