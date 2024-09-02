# ESTimeline

The distant goal is that anything you can do in SuperCollider could be sequenced on a timeline...

![screenshot](img/ss2.png)
<img src="img/sse.png" width="500" />

<img src="img/ssee1.png" width="400" /><img src="img/sss1.png" width="400" /><img src="img/rss.png" width="400" /><img src="img/ssep.png" width="400" />
<img src="img/fss.png" width="400" />

Timelines inside of timelines with optionally separate play clocks:

[![IMAGE ALT TEXT](http://img.youtube.com/vi/8jcxcfvS_08/0.jpg)](http://www.youtube.com/watch?v=8jcxcfvS_08 "Video Title")

## Features
- Non-prescriptive: no server architecture is forced on you, possible to disable timeline-specific clock and environment so as to interact with the timeline as part of a larger project; the basic goal is only to "execute this code at this particular time"
- Comment, Synth, Pattern, Routine, and Env clip types
  - Bulk editing selected clip parameters
  - Env clips can manage their own bus
  - Clips can reference other clips in the same timeline, to e.g. apply an Env to a Synth parameter
  - Pattern, Routine, Env, and Timeline clips can "fast forward" to start playing in the middle
    - (there is no way to fast forward a Synth, that I know of....)
  - Pattern and Routine clips can be seeded so random number generation is deterministic
  - Most fields can take a Function, so params can be generated on the fly
- Timeline Clip -- embed one timeline in another!
  - Each timeline clip can optionally use its own TempoClock, and optionally use its own Environment 
  - Each timeline (and timeline clip) has an init / free hook for e.g. allocating and freeing resources
- Tracks can contain all clip types, and can be muted/soloed
- Gray playhead is "scheduling playhead" and black playhead is "sounding playhead" -- to take into account server latency. Routines can be played with additional latency so non-sounding events line up with the sounding playhead.
- DAW-like GUI for editing and playback (see below for mouse and key commands lists)
- Undo and redo at each timeline level
- Easy to export timeline to IDE as plain text and load it back again

## Hypothetical features
- GUI editing
  - Snap to clips and/or to grid
  - Enhanced workflows and key commands for navigating and editing......
- Time features
  - Non-linearity: loop points, wait points, etc.
  - Indeterminacy: probability for clips not to play?
  - Clock follow: e.g. sync up with an Ableton timeline or midi show control
- Track/clips
  - Set default params per track (e.g. pan: -1)
  - Reference clips to create clones that all change together
- Envelopes
  - More live interaction - e.g. map a controller to a bus and record its movements to an envelope
  - Higher dimensional envelopes - e.g. movement through x/y space
- Playback and record audio files
  - easily access this Buffer for further manipulation
- Library integration
  - ddwPlug -- simplify bus routing for modulation
  - jitlib -- ditto
  - clothesline -- put whole .scd files on the timeline
  - VSTPlugin, somehow..... this could be a can of worms

## Issues
1. Although I've tried to make it pleasant, the GUI based code editing environment does not syntax highlight, autocomplete, etc -- for this reason I've added "Open in IDE" / "Copy from IDE" buttons as necessary.
  - Solution would be to someday add a Qt code view to core SC
2. When there are lots of quick zig-zags, high-resolution envelope drawing makes the GUI freeze up
  - to avoid this I have extremely pixelated the envelope drawing when zoomed in. Still looking for a good solution for this.
3. There is a limit to the complexity of a timeline created using SCLang (i.e. by evaluating `ESTimeline([ESTrack([....`) -- it may only contain max 256 functions.
  - to avoid this I have created a light custom file format that compiles complex timeline structures from the inside out

## Installing
Download or clone this repository into your SuperCollider Extensions directory. To see where this is, go to `File > Open user support directory` and find the `Extensions` directory, or evaluate:
```
Platform.userExtensionDir
```

## Mouse interaction
- drag middle of clip to move
- drag edges of clip to resize
- double click on clip to open editor window
- cmd-scroll zoom horizontally
- opt-scroll zoom vertically
- right click to see action menu
- Envelope breakpoint editing (cmd-e to toggle this mode)
  - click and drag to move breakpoints
  - shift-click to add breakpoint
  - opt-click to remvove breakpoint
- Selecting
  - click and drag to select both time and clips
    - hold cmd to just select clips
    - hold opt to just select time
  - hold shift to add clips to existing selection

## Key commands
- space toggles play
- s splits clip at mouse pointer
- delete deletes clip at mouse pointer
- e opens edit window for clip at mouse pointer, or init/cleanup func window for a timeline clip
- cmd-e toggles mouse editing of envelope breakpoints
- C inserts comment clip at mouse
- S inserts synth clip at mouse
- P inserts pattern clip at mouse
- R inserts routine clip at mouse
- E inserts env clip at mouse
- T inserts timeline clip at mouse
- cmd-t inserts new track after track at mouse
- cmd-T inserts new track before track at mouse
- cmd-delete deletes track at mouse
- cmd-a select all clips
- cmd-i inserts selected time
- shift-cmd-delete deletes selected time
- cmd-z undo
- cmd-Z redo

## Latest working test code

Empty timeline
```
(
~timeline.free;
~timeline = ESTimeline();
~window = ESTimelineWindow(timeline: ~timeline);
)
```

Test timeline with all elements
https://gist.github.com/esluyter/5aae7b7a15c2aa41cfdff990f0102c4e
