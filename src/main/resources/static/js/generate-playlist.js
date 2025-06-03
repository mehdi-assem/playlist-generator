  // Updated JavaScript to work with your existing functions
  function selectMode(mode) {
      // Update radio buttons (your existing logic)
      document.getElementById('inspired').checked = mode === 'inspired';
      document.getElementById('custom').checked = mode === 'custom';
      document.getElementById('freeform').checked = mode === 'freeform';

      // Show/hide customization options (your existing logic)
      const customOptions = document.getElementById('customization-options');
      const freeformOptions = document.getElementById('freeform-options');

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

  function toggleSection(sectionName) {
      const content = document.getElementById(sectionName + '-content');
      const toggle = document.getElementById(sectionName + '-toggle');

      if (content.classList.contains('expanded')) {
          content.classList.remove('expanded');
          toggle.classList.remove('expanded');
      } else {
          content.classList.add('expanded');
          toggle.classList.add('expanded');
      }
  }

  function selectMood(element, mood) {
      // Remove selection from all mood buttons (your existing logic)
      document.querySelectorAll('.mood-button').forEach(btn => {
          btn.classList.remove('selected');
      });

      // Add selection to clicked button (your existing logic)
      element.classList.add('selected');
      document.getElementById('selected-mood').value = mood;
  }

  function quickGenerate(type) {
      // Set mode to inspired (your existing logic)
      selectMode('inspired');

      // Add quick generation type (your existing logic)
      let input = document.createElement('input');
      input.type = 'hidden';
      input.name = 'quickType';
      input.value = type;
      document.getElementById('playlistForm').appendChild(input);

      // Submit form (your existing logic)
      document.getElementById('playlistForm').submit();
  }

  // Auto-expand first section when custom mode is selected (your existing logic)
  document.getElementById('custom').addEventListener('change', function() {
      if (this.checked) {
          setTimeout(() => {
              toggleSection('mood');
          }, 300);
      }
  });

  // Your existing form submission logic
  document.getElementById('playlistForm').addEventListener('submit', function (e) {
      document.getElementById('loading-overlay').style.display = 'flex';
      this.querySelector('.generate-btn').disabled = true;
  });

  // Toggle listening history timeframe based on checkbox (new functionality)
  document.addEventListener('DOMContentLoaded', function() {
      const checkbox = document.getElementById('use-listening-history');
      const timeframe = document.getElementById('history-timeframe');

      if (checkbox && timeframe) {
          checkbox.addEventListener('change', function() {
              timeframe.style.display = this.checked ? 'block' : 'none';
          });
      }
  });