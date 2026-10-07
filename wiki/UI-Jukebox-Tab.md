# Jukebox Tab

<img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> Your own music, played
from your own files, underneath Vega rather than over her. Whenever Vega speaks, the music dips
automatically and comes back up when she is done — you never miss a callout.

Nothing to install, no account, no streaming service. The tab is laid out top to bottom:
**Music Library**, **Playlist**, **Playback**.

---

## Music Library

Where the music comes from.

- **Browse...** — choose a music folder. Elite Intel walks it, including sub-folders, and adds
  every playable file it finds to the playlist. Files are added in folder order, so audiobook
  chapters arrive in sequence.
- **Rescan** — look through the folder again for files you have added since.

**Supported formats:** MP3, FLAC, M4A / M4B (AAC), OGG / OGA (Vorbis) and WAV. Files the
Jukebox cannot play (WMA, Apple Lossless, DRM-protected purchases, Opus) are skipped.

---

## Playlist

The playlist is the queue: what you see is the order it plays in.

| Column | Meaning |
|--------|---------|
| **#** | Position. A ▶ marks the track that is playing |
| **Title** · **Artist** · **Album** | Read from the files' own tags. A big library fills in over a few seconds |
| **Time** | Track length |

- **Double-click** a track to play it.
- **Drag** rows to reorder them. The order is saved.
- Clicking a column header does **not** sort — that would throw away an order you arranged by
  hand. Sorting is in the right-click menu instead.

A file that has disappeared from disk is marked **missing**.

### Right-click menu

| Item | What it does |
|------|--------------|
| **Play now** | Play the selected track |
| **Play next** | Move the selected tracks to play straight after the current one |
| **Remove from playlist** | Remove them from the list (the files on disk are not touched) |
| **Show in file manager** | Open the folder holding the file |
| **Copy artist and title** | To the clipboard |
| **Add folder...** | Add another folder's music |
| **Import playlist...** | Add the tracks an `.m3u` / `.m3u8` playlist names |
| **Remove missing files** | Drop every entry whose file is gone |
| **Clear playlist** | Remove every track (asks first; files on disk are not deleted) |
| **Sort by** → Title / Artist / Folder | A one-off sort that rewrites the playlist order |

---

## Playback

- **The play-head** — how far into the track you are. Drag it to seek; playback jumps when you
  let go.
- **Previous · Play/Pause · Stop · Next** — the transport. **Stop** rewinds the current track
  to the start; **Pause** keeps your place.
- **Order** — *Sequential* or *Random*.
- **Volume** — the music's own level. It is here, not on the Audio settings tab, so you never
  turn Vega down by mistake.

Your place in a track is remembered between sessions — handy for audiobooks — but the Jukebox
never starts playing on its own when the app launches.

---

## Voice commands

Every music command names *music*, a *track* or a *song*, so it never collides with ship
commands like "stop" or "next".

| Say | What happens |
|-----|--------------|
| *"Play music"* / *"resume the music"* | Start or resume |
| *"Pause the music"* / *"stop the music"* | Pause — "play music" picks up where it left off |
| *"Next track"* / *"skip this song"* | Next track |
| *"Previous track"* | Previous track |
| *"Restart the playlist"* | Back to the first track |
| *"Shuffle the music"* / *"play the music in order"* | Random or sequential order |
| *"Play the song Rocket Man"* | Find a track by its title or artist and play it. If nothing matches closely, Vega says so rather than playing the wrong thing |

These also work typed in the game chat — see [All Commands](AllCommands).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
