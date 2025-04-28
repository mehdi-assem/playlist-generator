// LandingPage.js

// Function to perform the Spotify login fetch and redirect the browser
function getSpotifyUserLogin() {
fetch('http://localhost:8080/api/login')
    .then(response => response.text())
    .then(url => {
        window.location.href = url;
    })
    .catch(error => {
        console.error('Error fetching Spotify login URL:', error);
    });
}

// Function to change the background color of the container
function changeBackgroundColor() {
  const container = document.getElementById("root");
  container.style.backgroundColor = "#f0f8ff"; // Light blue color
}

// Function to initialize the landing page content
function initLandingPage() {
  const root = document.getElementById("root");

  // Create the heading element
  const heading = document.createElement("h1");
  heading.textContent = "Please log in with Spotify to get started";

  // Create the button element
  const button = document.createElement("button");
  button.textContent = "Sign In";

  // Add a click event listener to the button to change background and trigger login
  button.addEventListener("click", function() {
    getSpotifyUserLogin();
  });

  // Clear any existing content and append the new elements
  root.innerHTML = "";
  root.appendChild(heading);
  root.appendChild(button);
}

// Initialize the landing page once the DOM is fully loaded
document.addEventListener("DOMContentLoaded", initLandingPage);
