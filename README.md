# Spotify Playlist Generator

An intelligent playlist generator that creates personalized Spotify playlists using AI and your listening history. Built with Spring Boot, powered by Google Gemini AI and LastFM API, this application analyzes your music preferences and generates curated playlists based on your taste, mood, and custom parameters.

## Features

- **Smart Playlist Generation**: AI-powered playlist creation using Google Gemini 2.0 Flash
- **Listening Analytics**: View your top artists and tracks across different time periods
- **Multiple Generation Modes**:
  - **Get Inspired**: Automatic generation based on your listening history
  - **Customize**: Fine-tune with mood, genre, decade, and artist preferences
  - **Free Input**: Natural language playlist requests
- **Artist Discovery**: Find similar artists using Last.fm integration
- **Spotify Integration**: Seamless OAuth authentication and playlist creation
- **Time-based Analysis**: 4 weeks, 6 months, or long-term listening history
- **Paginated Views**: Easy navigation through your music library

## Tech Stack

### Backend
- **Java** with **Maven**
- **Spring Boot** - Application framework
- **Thymeleaf** - Template engine
- **MVC Architecture**

### Frontend
- **HTML/CSS/JavaScript** - User interface
- **Responsive Design**

### APIs & Services
- **Spotify Web API** - Music data and playlist management
- **Last.fm API** - Artist similarity and music discovery
- **Google Gemini 2.0 Flash** - AI-powered playlist generation

## Prerequisites

Before running this application, you'll need:

- **Java 23+** installed
- **Maven 4+** installed
- **Spotify Developer Account** for API access
- **Last.fm API Account** for music discovery features
- **Google AI Studio Account** for Gemini API access

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/mehdi-assem/playlist-generator.git
cd playlist-generator
```

### 2. API Credentials Setup

You'll need to obtain API credentials from multiple services:

#### Spotify API Setup
1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Create a new application
3. Note your `Client ID` and `Client Secret`
4. Add your redirect URI (e.g., `http://localhost:8080/callback`)

#### Last.fm API Setup
1. Visit [Last.fm API Account Creation](https://www.last.fm/api/account/create)
2. Create an API account
3. Get your `API Key` and `Shared Secret`

#### Google Gemini API Setup
1. Go to [Google AI Studio](https://aistudio.google.com/)
2. Create a new project or select existing one
3. Generate an API key for Gemini 2.0 Flash
4. Copy your API key

### 3. Configuration Files

Create or configure the following files:

#### `src/main/resources/application.properties`
```properties
spring.config.import=classpath:env.properties

server.port=${SERVER_PORT}

custom.server.ip=${CUSTOM_SERVER_IP}
redirect.server.ip=${REDIRECT_SERVER_IP}

artists.redirect.url=${ARTISTS_REDIRECT_URL}
generate.playlist.redirect.url=${GENERATE_PLAYLIST_REDIRECT_URL}

spring.mvc.static-path-pattern=/**

spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.enabled=true

# Spotify API Credentials
spotify.redirectUri=${SPOTIFY_REDIRECT_URI}
spotify.clientId=${SPOTIFY_CLIENT_ID}
spotify.clientSecret=${SPOTIFY_CLIENT_SECRET}

# LastFM API Credentials
lastfm.redirectUri=${LASTFM_REDIRECT_URI}
lastfm.apiKey=${LASTFM_API_KEY}
lastfm.sharedSecret=${LASTFM_SHARED_SECRET}

# Google Gemini API Credentials
google.gemini.apiKey=${GOOGLE_GEMINI_API_KEY}
```

#### `src/main/resources/env.properties`
```properties
SERVER_PORT=8080
CUSTOM_SERVER_IP=127.0.0.1
REDIRECT_SERVER_IP=127.0.0.1
ARTISTS_REDIRECT_URL=http://localhost:8080/artists
GENERATE_PLAYLIST_REDIRECT_URL=http://localhost:8080/api/generate-playlist

SPOTIFY_REDIRECT_URI=http://localhost:8080/callback
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret

LASTFM_REDIRECT_URI=http://localhost:8080/callback
LASTFM_API_KEY=your_lastfm_api_key
LASTFM_SHARED_SECRET=your_lastfm_shared_secret

GOOGLE_GEMINI_API_KEY=your_google_gemini_api_key
```

#### `src/main/resources/static/js/config.js`
```javascript
window.config = {
    apiBaseUrl: 'http://localhost:8080/api',
    redirectServerIp: 'http://localhost:8000'
};
```

### 4. Build and Run

```bash
# Build the application
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

## Usage Guide

### Getting Started
1. **Launch the app** and click "Continue with Spotify"
2. **Authorize** the application through Spotify OAuth
3. **Choose your playlist generation mode**:
   - **Get Inspired**: Quick generation based on your history
   - **Customize**: Set specific parameters (mood, genre, decade, artists)
   - **Free Input**: Describe your ideal playlist in natural language

### Playlist Generation Options
- **Listening History**: Toggle on/off and select timeframe
- **Artist Selection**: Include exact artists, similar artists, or both
- **Custom Parameters**: Fine-tune with mood, genre, and decade filters
- **AI Prompts**: Use natural language to describe your playlist criteria

### Exploring Your Music
- **Artists Page**: View your top artists with time period filters
- **Tracks Page**: View your top tracks with time period filters
- **Playlists Page**: View all your created playlists

## Project Structure

```
src/
├── main/
    ├── java/
    │   └── com/playlistgenerator/
    │       ├── controller/     # REST controllers
    │       ├── service/        # Business logic
    │       ├── handler/        # Handler classes
    │       ├── config/         # Configuration classes
    │       ├── config/         # Configuration classes
    │       ├── enums/          # Enumeration classes
    │       ├── enums/          # Exception handler classes
    │       └── dto/            # Data Transfer Object
    └── resources/
        ├── templates/          # Thymeleaf templates
        ├── static/
        │   ├── css/           # Stylesheets
        │   └── js/            # JavaScript files
        ├── application.properties.example
        └── env.properties.example
```

## Security Notes

- Never commit API keys to version control
- Use environment variables for production deployment
- Ensure redirect URIs match your registered app settings
- Keep your Spotify app in development mode for personal use

## Acknowledgments

- [Spotify Web API Java Wrapper](https://github.com/spotify-web-api-java/spotify-web-api-java) for simplified Spotify integration
- [Last.fm API](https://www.last.fm/api) for music discovery features
- [Google Gemini](https://ai.google.dev/) for AI-powered playlist generation

---
