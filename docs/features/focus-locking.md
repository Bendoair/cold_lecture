# Focus locking

## What it is for

A lecture is easy to lose to one quick tab switch. Focus locking turns the study app into the only thing on screen for the length of a lecture, and makes leaving a deliberate choice instead of a reflex.

## What the student can do

- **Lock in** from the top bar (or the play button next to the session timer). The app goes fullscreen and a focus session starts.
- See a **focus streak** (how long they have stayed without leaving) and a **session timer** (how long the session has been running) in the top bar.
- **Unlock** by clicking *Locked*. Unlocking asks them to retype a short random code. The code is easy to read but takes a moment to type, which is the point: it gives a second to reconsider.
- Choose **Stay focused** on the unlock screen to go straight back.

## When the student leaves anyway

If they switch to another window or tab, or close fullscreen, the app notices. When they come back, it shows how long they were gone. Their streak is paused and starts over from zero, while the session timer keeps running. They return with one click (*Back to the lecture*) or end the session from the same screen.

The unlock screen and the "you stepped away" screen both show the session length, the best streak and how many times the student left.

## How strong the lock is, per platform

The app always tells the student which kind of lock they are in. The bottom bar shows *Focus: Desktop lock* or *Focus: Soft lock (browser)*.

| | Desktop app | Browser |
|---|---|---|
| Fullscreen while locked | Yes | Yes, but the browser always lets Esc exit fullscreen |
| Stays above other windows | Yes, while locked | No |
| Closing the app while locked | Shows the unlock code first | The browser's own "leave site?" prompt |
| Detects leaving | Yes (window loses focus) | Yes (tab hidden, window blurred, fullscreen exited) |

## What it is not

Focus locking is friction, not enforcement. It is not a kiosk mode and it cannot stop someone who is determined to leave: the operating system can still switch apps, and a browser tab can always be closed. Its goal is to make leaving visible and slightly effortful, and to make coming back easy.
