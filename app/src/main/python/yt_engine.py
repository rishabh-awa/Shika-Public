import os
import tempfile
from yt_dlp import YoutubeDL

# Ensure temporary files have write permissions on device environments
os.environ["TMPDIR"] = tempfile.gettempdir()

YTDL_OPTIONS = {
    'format': 'bestaudio/best',
    'noplaylist': True,
    'extract_flat': True,                 # Keeps search extraction lightning fast on local devices
    'nocheckcertificate': True,           # Fixes Android root SSL certificate validation issues
    'quiet': True,
    'no_warnings': True,
    'source_address': '0.0.0.0',          # Forces IPv4 extraction pathways
}

def get_clean_stream_url(video_id):
    """
    Called when a song needs to actually play.
    We swap extract_flat to False to completely resolve the final audio stream source.
    """
    video_url = f"https://www.youtube.com/watch?v={video_id}"
    play_opts = YTDL_OPTIONS.copy()
    play_opts['extract_flat'] = False

    with YoutubeDL(play_opts) as ydl:
        try:
            info = ydl.extract_info(video_url, download=False)
            return info.get('url')
        except Exception:
            return None

def search_youtube_tracks(query_text):
    """
    Queries YouTube and explicitly extracts the flat structure fields.
    """
    search_query = f"ytsearch5:{query_text}"
    with YoutubeDL(YTDL_OPTIONS) as ydl:
        try:
            info = ydl.extract_info(search_query, download=False)
            tracks = []

            for entry in info.get('entries', []):
                if entry:
                    title = entry.get('title') or "Unknown Title"
                    artist = entry.get('uploader') or entry.get('channel') or "Unknown Artist"
                    video_id = entry.get('id') or ""

                    # Clean up common "Artist - Track" title double-mappings
                    if " - " in title and artist == "Unknown Artist":
                        parts = title.split(" - ", 1)
                        artist = parts[0].strip()
                        title = parts[1].strip()

                    tracks.append({
                        'id': str(video_id),
                        'title': str(title),
                        'artist': str(artist)
                    })
            return tracks
        except Exception:
            return []