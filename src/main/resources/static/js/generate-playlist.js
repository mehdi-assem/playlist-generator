// generate-playlist.js - Only for the playlist generation page
document.addEventListener('DOMContentLoaded', function() {
    // Make sure we're on the right page
    const playlistForm = document.getElementById('playlistForm');
    if (!playlistForm) {
        return; // Exit if we're not on the generate playlist page
    }

    // Generic function to handle item selection/deselection
    function handleItemClick(itemName, itemCard, inputFieldName) {
        const inputField = document.querySelector(`input[name="${inputFieldName}"]`);
        let currentItems = inputField.value.split(',').map(item => item.trim()).filter(item => item);

        // Escape commas and hyphens in the item name for comparison
        const escapedItemName = itemName.replace(/[,-]/g, m => '\\' + m);
        const isSelected = currentItems.some(item => {
            const escapedItem = item.replace(/[,-]/g, m => '\\' + m);
            return escapedItem === escapedItemName;
        });

        if (isSelected) {
            // Remove the item from the input field
            currentItems = currentItems.filter(item => {
                const escapedItem = item.replace(/[,-]/g, m => '\\' + m);
                return escapedItem !== escapedItemName;
            });
            itemCard.classList.remove('selected');
        } else {
            // Add the item to the input field
            currentItems.push(itemName);
            itemCard.classList.add('selected');
        }

        inputField.value = currentItems.join(', ');
    }

    // Function to handle artist image click
    function handleArtistClick(artistName, artistCard) {
        handleItemClick(artistName, artistCard, 'artists');
    }

    // Function to handle track image click
    function handleTrackClick(trackName, trackCard) {
        handleItemClick(trackName, trackCard, 'tracks');
    }

    // Function to handle album image click
    function handleAlbumClick(albumName, albumCard) {
        handleItemClick(albumName, albumCard, 'albums');
    }

    // Function to fetch and display items
    function fetchAndDisplayItems(url, gridId, itemType) {
        const timeframe = document.getElementById('timeframe').value;
        const useListeningHistory = document.getElementById('use-listening-history').checked;

        if (!useListeningHistory) {
            document.getElementById(gridId).innerHTML = `<p>Please enable listening history to see your top ${itemType}.</p>`;
            return;
        }

        fetch(`${url}?timeRange=${timeframe}`)
            .then(response => response.json())
            .then(data => {
                const grid = document.getElementById(gridId);
                grid.innerHTML = '';

                data.items.forEach(item => {
                    const itemCard = document.createElement('div');
                    let itemName, itemImageUrl, itemCardClass, clickHandler;

                    if (itemType === 'artists') {
                        itemName = item.name;
                        itemImageUrl = item.images[0].url;
                        itemCardClass = 'artist-card';
                        clickHandler = () => handleArtistClick(item.name, itemCard);
                    } else if (itemType === 'tracks') {
                        itemName = `${item.name} - ${item.artists[0].name}`;
                        itemImageUrl = item.album.images[0].url;
                        itemCardClass = 'track-card';
                        clickHandler = () => handleTrackClick(item.name, itemCard);
                    } else if (itemType === 'albums') {
                        itemName = item.album.name;
                        itemImageUrl = item.album.images[0].url;
                        itemCardClass = 'album-card';
                        clickHandler = () => handleAlbumClick(item.album.name, itemCard);
                    }

                    itemCard.className = itemCardClass;
                    itemCard.innerHTML = `
                        <img src="${itemImageUrl}" alt="${itemName}">
                        <h2>${itemName}</h2>
                    `;
                    itemCard.addEventListener('click', clickHandler);
                    grid.appendChild(itemCard);
                });
            })
            .catch(error => console.error(`Error fetching top ${itemType}:`, error));
    }

    // Function to fetch and display artist images and names
    function fetchAndDisplayArtists() {
        fetchAndDisplayItems('/api/top-artists', 'artist-grid', 'artists');
    }

    // Function to fetch and display track images and names
    function fetchAndDisplayTracks() {
        fetchAndDisplayItems('/api/top-tracks', 'track-grid', 'tracks');
    }

    // Function to fetch and display album images and names
    function fetchAndDisplayAlbums() {
        fetchAndDisplayItems('/api/top-tracks', 'album-grid', 'albums');
    }

    // Function to select mode
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

    // Function to toggle sections
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

    // Function to select mood
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

    // Function to quick generate
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

    // Add event listeners for timeframe changes
    const timeframeSelect = document.getElementById('timeframe');
    if (timeframeSelect) {
        timeframeSelect.addEventListener('change', function() {
            fetchAndDisplayArtists();
            fetchAndDisplayTracks();
            fetchAndDisplayAlbums();
        });
    }

    // Add event listener for listening history checkbox changes
    const checkbox = document.getElementById('use-listening-history');
    if (checkbox) {
        checkbox.addEventListener('change', function() {
            const timeframe = document.getElementById('history-timeframe');
            if (timeframe) {
                timeframe.style.display = this.checked ? 'block' : 'none';
            }
            fetchAndDisplayArtists();
            fetchAndDisplayTracks();
            fetchAndDisplayAlbums();
        });
    }

    // Call the functions to fetch and display the images and names when the page loads
    fetchAndDisplayArtists();
    fetchAndDisplayTracks();
    fetchAndDisplayAlbums();

    // Make functions globally available for onclick handlers in HTML
    window.selectMode = selectMode;
    window.toggleSection = toggleSection;
    window.selectMood = selectMood;
    window.quickGenerate = quickGenerate;
});
