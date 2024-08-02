# ESTimeline

The distant goal is that anything you can do in SuperCollider could be sequenced on a visual and editable timeline...

Note that this is a work in progress and all is subject to revision. I am trying if at all possible to keep the saved timeline files valid.

Also note that because timelines are built to execute user-supplied code they are inherently insecure. Before opening timeline files from another source, open them in a text editor to make sure they don't have malicious code. (i.e. treat them just like any other random SC code you are executing for the first time)
<br />
<br />
<details>
  <summary>Out of date screenshots</summary>
  <img src="img/ss2.png" />
  <img src="img/sse.png" width="500" />
  
  <img src="img/ssee1.png" width="400" /><img src="img/sss1.png" width="400" /><img src="img/rss.png" width="400" /><img src="img/ssep.png" width="400" />
  <img src="img/fss.png" width="400" />
</details>

<details>
  <summary>Out of date video demo: Timelines inside of timelines with optionally separate play clocks</summary>
  http://www.youtube.com/watch?v=8jcxcfvS_08
</details>

<br />
<br />

<details>
  <summary><strong>Features, hypothetical features, and issues</strong></summary>
  
## Features
- **Non-prescriptive:**
  - the basic goal is only to "execute this code at this particular time"
    - although the competing goal is to make it easy to do the things you want to do, which is subjective
  - for the moment just real-time but some of this could be translated easily to work NRT
    - there are certain things impossible in NRT, i.e. to do with real-time input
    - ddwMixerChannel doesn't yet work NRT so this would be a big rewrite
  - as little architecture as possible is forced on you
    - possible to disable ddwMixerChannel, timeline-specific clock and environment so as to interact with the timeline as part of a larger project
    - possible to play clips with any bus, target, addAction, etc. for full flexibility
- **DAW-like GUI** for editing and playback
  - The goal is an accurate visual representation of what you are hearing / when the code is executed
    - gray playhead is "scheduling playhead" and black playhead is "sounding playhead" -- to take into account server latency
    - Routines can be played with additional latency so non-sounding events line up with the sounding playhead
  - Keyboard and mouse interface to full extent of Timeline capabilities, with built-in code editing
  - Snap to grid optional
  - Optional full GUI mixing interface using ddwMixerChannel
    - track insert FX, pre fade sends and post fade sends
    - automate mixer channel parameters (level, pan, sends, fx parameters) with editable envelopes
- **Non-linear:** "goto" command to jump to a clip or a point in time enabling complex real-time behaviors (variable-length looping, conditional branching...
- **Tracks** are the main form of organization of clips
  - tracks can contain any type of clip in any combination
  - tracks can be muted/soloed and rearranged
  - individual clips can be muted
  - if using ddwMixerChannel, tracks will play on a mixer channel specified by the track's name
    - sub timelines will play on mixer channels feeding into their parent track's mixer channel
- **Synth, Pattern, Routine, and Env** clip types
  - Synth clips can either instantiate a SynthDef or run their own single-use function a la `{ }.play`
    - You can select multiple Synth clips and bulk edit their arguments
  - Env clips play on a bus and come with a shortcut to map to Synths and Patterns, and 
    - can optionally manage their own bus, and
      - if so, all Env clips with the same name on a timeline share the same bus
  - Clips can reference other clips in the same timeline by name
    - from a routine, `goto` any clip by name
    - apply an Env to a Synth parameter or use it in a Pattern or Routine
    - if more than one clip share the same name, the referenced clip is the closest to the playhead
  - Pattern, Routine, Env, and Timeline clips can "fast forward" to start playing in the middle
    - (there is no way to fast forward a Synth, that I know of....)
  - Pattern and Routine clips can be seeded so random number generation is deterministic
  - Most fields can take a Function, so params can be generated on the fly
- **Timeline Clip** -- embed one timeline in another!
  - Each timeline clip can optionally use its own TempoClock, and optionally use its own Environment 
  - Each timeline (and timeline clip) has an init / free hook for e.g. allocating and freeing resources
- **Undo and redo** at each timeline level
- **Save and recall**
  - Save as plain text files in SC array format
  - Once you have saved, the timeline will update a backup file every time you add an undo point, in case of crash
 
## Issues
1. Although I've tried to make it pleasant, the GUI based code editing environment does not syntax highlight, autocomplete, etc -- for this reason I've added "Open in IDE" / "Copy from IDE" buttons as necessary.
    - Solution would be to someday add a Qt code view to core SC
2. ~~When there are lots of quick zig-zags, high-resolution envelope drawing makes the GUI freeze up~~
    - ~~to avoid this I have extremely pixelated the envelope drawing when zoomed in. Solution would be to someday add a better Qt envelope view to core sc.~~
    - I have improved this by drawing envelopes as images. Still not perfect.
3. I would have liked to have saved the timeline files as executable SCLang just as you would write by hand; however:
    - There is a limit to the complexity of a timeline created using SCLang (i.e. by evaluating `ESTimeline([ESTrack([....`) -- it may only contain max 256 functions.
    - to avoid this I have created a light custom file format that compiles complex timeline structures from the inside out
4. At high track counts, it takes a little while to load and free all the MixerChannels.
    - I have tried to reduce the occasions on which this needs to happen.
5. Changes will generally not take effect until you've stopped and restarted playback. This will be difficult to fix, but someday I hope to.

## Hypothetical features
These are all things I would like to implement someday:
- NRT: this will be difficult unless ddwMixerChannel gains NRT support
- Clock follow: e.g. sync up with an Ableton timeline or midi show control
- More clip types
  - audio file
    - possible to record input or bounce tracks to audio clip in real time
  - loop
  - OSCdef
  - midi/piano/drum roll
  - "clones" that change with their parents
- Envelope improvements
  - Ability to draw freehand with mouse 
  - More live interaction - e.g. map a controller to a bus and record its movements to an envelope
  - Higher dimensional envelopes - e.g. movement through x/y space
  - Timeline tempo envelopes (this is already possible but kind of annoying, using an Env clip and a Routine clip
- Library integration
  - VSTPlugin for adding VST effects to mixing chain
  - ddwPlug -- simplify bus routing for modulation
  - clothesline -- put whole .scd files on the timeline

<br />
</details>

<details>
  <summary><strong>Getting started: installing and tutorial</strong></summary>
  <br /><br />
<!--Here is a slightly out of date tutorial in that it doesn't use MixerChannel. All parts are still relevant except "Environment variables - adding reverb" -- which is useful to demonstrate how the timeline works but there is a much better way in the form of mixer channels, which I will soon update this tutorial to encompass. -->
This tutorial uses mac keyboard shortcuts. I believe for other platforms you can substitute ctrl and alt.
  
## Installing
Download or clone this repository into your SuperCollider Extensions directory. To see where this is, go to `File > Open user support directory` and find the `Extensions` directory, or evaluate:
```
Platform.userExtensionDir
```

## Tutorial: basic workflow examples
```
(
~timeline = ESTimeline(bootOnPrep: true, useMixerChannel: false);
~window = ESTimelineWindow(timeline: ~timeline);
)
```
- this boots the default server, but does not use ddwMixerChannel.

### Tracks:
- Tracks are the main form of clip organization.
- click anywhere in the timeline to make sure it is focused
- press cmd-t to add a track after the one your mouse is currently over, or shift-cmd-T to add it before the current track
- double click in the left panel to rename tracks
- cmd-delete deletes the track under your mouse
- mute and solo tracks using the buttons on the left panel
- click and drag in the left panel to rearrange tracks

### Synth Clips:
- create a bunch of Synth clips (point the mouse where you want it and press shift-S, or use right click menu)
- press the spacebar to play your clips
  - the gray playhead is the "scheduling playhead" -- this is when the code is executed
  - the black playhead is the "sounding playhead" -- because of server latency, this is when the events actually sound. this is generally the one you want to watch
  - press space again to stop playback
- drag them around to move them in time or between tracks
  - they will always snap to the playhead in time
    - to move playhead to beginning of clip, make sure your mouse is inside of clip and press [
  - check the `snapToGrid` box or press opt-s to align your edits with the tempo grid
  - drag their edges to resize them (a red bar appears when you are within the resize zone)
  - option-drag to copy a clip
- double-click on a clip to edit it
  - double-click on the grayed out `freq` parameter to activate it, then you can set it to any valid SuperCollider expression, like `220` or `60.midicps`
  - press save when you're done, and close the edit window if you want.
- if you play now by clicking to place the playhead and pressing space, you will hear they play the default synth with your parameters at the appropriate time

### Scrolling and zooming:
- use trackpad to scroll left and right or click and drag ruler at top
- cmd-scroll to zoom in and out horizontally
- opt-scroll to zoom in and out vertically

### SynthDefs:
- put your SynthDef in the timeline's prep function (click the "edit prep/cleanup funcs" button) e.g.
```
SynthDef(\sin, { |out, freq = 440, gate = 1, amp = 0.1, preamp = 1.5, attack = 0.001, release = 0.01, pan, verbbus, verbamt, vibrato = 0.2|
  var env, sig;
  var lfo = XLine.ar(0.01, vibrato, ExpRand(0.5, 2.0)) * SinOsc.ar(5.4 + (LFDNoise3.kr(0.1) * 0.5));
  gate = gate + Impulse.kr(0);
  env = Env.adsr(attack, 0.1, 0.4, release).ar(2, gate);
  sig = SinOsc.ar(freq * lfo.midiratio) * env;
  sig = (sig * preamp).tanh;
  sig = Pan2.ar(sig, pan, amp);
  Out.ar(out, sig);
  Out.ar(verbbus, sig * verbamt);
}).add;
```
- hit save when you're done to save the prepFunc and load it. close the window, if you want

### Bulk edit synth clips:
- click in an empty area and drag to select all the Synth clips (they will be highlighted in cyan when selected)
- right click, "clip actions > bulk actions > Bulk edit Synth defName"
  - and set them to `'sin'` and hit ok
- play again and you hear they now all play your SynthDef
  - double-click in an empty area to remove selection
  - double-click on a clip now and you will see all the new parameters you can control.

### Envelopes for Synth parameters:
- right click a Synth clip, "clip actions > synth actions > add env for synth argument"
- pick "freq" from the list and hit OK
  - this will add a new track above your clip with an envelope clip on it that is the length of your Synth clip
    - with a unique name (starting from 'freq0'),
    - initialized with the current value of that parameter
    - and it will update the freq argument of the Synth clip to read from this envelope's bus

### Editing Envelopes:
- cmd-e to enter envelope breakpoint editor mode
  - click and drag on a breakpoint to move it around,
  - click and drag between breakpoints to adjust the curve,
  - shift-click to add breakpoints,
  - option-click to remove them
  - to adjust the envelope range, right click, clip actions > env actions > "set env range keeping breakpoint values"
    - now you can change the frequency range of the envelope, say min `100` max `5000`
    - ok to save changes -- this will keep your values intact so long as they fall within the new range
- hit cmd-e again to leave envelope breakpoint editor mode

### Bulk edit Synths -- To make this envelope affect all your Synths:
- click and drag to select all the Synth clips (your envelope clip can also be selected, it doesn't matter)
- right click, "clip actions > bulk actions > Bulk edit (change) Synth arguments"
- assign the `freq` of all the clips to (the single quotes are important!) 
`'freq0'`
(or whatever the name of the envelope clip is)
- you should see all their freqs change to show the audio rate bus that the Env clip has created for you (for me this is a8)
- double click to deselect all clips, then:
- drag the edges of the envelope clip to resize it, so that it covers the entire range of your Synth clips
- cmd-e to edit the breakpoints again
- you should hear it is now controlling all the synths' pitches
- make sure you've left breakpoint edit mode when you want to move clips around

### Bulk adjust Synths -- Random panning:
- set one of your clips to pan hard left by double-clicking and setting its pan to -1
- Select all your Synth clips
- right click > clip actions > bulk actions > Bulk adjust (modify) Synth arguments
- for `pan` put in `+ 0.5.rand2` and check the "hard coded" box
  - this will generate a random pan per clip that is within 0.5 of its original panning. (if you want it to be newly random every time you play it, uncheck the "hard-coded" box)

### Pattern Clips:
- make a new track and shift-P to make a pattern clip
- double click to edit, e.g.:
```
Pbind(
  \instrument, \sin,
  \degree, Pbrown(0, 7 * 3 + 1, 3),
  \octave, Pdup(Pwhite(1, 10), Pwhite(3, 5)),
  \dur, Pbrown().linexp(0, 1, 0.02, 1.0),
)
```
- save and play
- if you want to try a new random seed, click "re-roll" button and save
  - you can always undo if you don't like it (cmd-z undo, shift-cmd-Z redo)
- you can drag the edges to adjust start and end point without changing the timing of the notes
  - you can split it into two by pointing with the mouse where you want the split and pressing s
- if you make a new track and a new envelope (shift-E),
  - double click on the envelope, name it `pan0` and set its range from -1 to 1
  - click "save" to save it, and close the window if you want
- double click on each of the pattern clips and add
```
  \pan, ~thisTimeline[\pan0],
```
- cmd-e and edit the panning to your liking

### Saving
- click "save as" button or hit cmd-s
- navigate to the directory you want to save in, and enter a file name to save as (e.g. "tutorial0.scd")
    
### Timeline clips:
- above the main timeline, click "Open as clip in new timeline"
  - Now this little system, the synths, patterns, buses and envelopes, are all encapsulated in this timeline clip
    - (in fact you can duplicate the timeline clip by option-dragging onto a new track, and the two will play simultanously each using its own environment and buses)
  - you can also resize the clips, move the mouse cursor over the clip and use the s key to split it into two separate timeline clips, etc.
 
### Mixer channels:
This will now assume you have ddwMixerChannel installed. (`Quarks.install("ddwMixerChannel")`)
- check the `useMixerChannel` box
- show the mixer window: evaluate this in the IDE
```
~mixerWindow = ESMixerWindow(~timeline, ~window);
```
- click back on the timeline and press play
  - you will now see a mixer on which currently playing channels are visible
    - subtimelines feed into their parent track's channel
  - you can move these faders and pan knobs around to mix the inputs
   
### Adding reverb:
- create a new track by pressing cmd-T while your mouse is over the last track
- double click on the left panel to name it `verb` and press enter.
- right-click one of the gray rectangles above the "verb" channel strip on the mixer
- select "new insert fx"
- replace the function with:
```
{ |time = 2.5|
  var sig = In.ar(~out, 2);
  NHHall.ar(sig, time);
}
```
- save and close window, if you want
- right click on one of the gray rectangles above one of the tracks with a timeline clip on it and select "new send"
  - it defaults to `'verb'` at 0db
  - click ok
  - you will hear the timelines on this track play with reverb, other tracks not
  - use its slot on the mixer interface to tune levels

Automating mixer levels, send levels, and fx parameters left as an exercise, for now...

### Using Routine clips:
- shift-R to make a Routine clip, double click to edit
- You can think of Routine clips as kind of your generic "execute this code here"
- to jump to beat 1 on this timeline, use
```
~thisTimeline.goto(1)
```
- to jump to a clip named `next`, use
```
~thisTimeline.goto(\next)
```
  - you can use a comment clip (shift-C) for this dummy "next" clip -- the first line of the comment is its name
- it's important to use `s.bind` for server operations inside of routines, otherwise the timing is off.
```
var syn;
10.do { |i|
  s.bind { syn = Synth(\default, [freq: (40 + i).midicps]) };
  0.2.wait;
  s.bind { syn.free };
  0.2.wait;
};
```
- if you want say OSC out to a light board to line up with the sounding events, check the `addLatency` box.
- You can interact with the timeline using `~thisTimeline` which always refers to the timeline you're currently working in, or `~timeline` which refers to either this or the nearest parent timeline whose `useEnvir` box is checked
  - if no parent timeline is set to `useEnvir`, then `~thisTimeline` will overwrite anything you might have in your current environment.
  - in that case, `~timeline` might be nil unless you've set it in your current environment.
- to get the current value of an envelope named `env` from within a routine:
```
loop {
  ~thisTimeline[\env].valueNow.postln;
  1.wait;
};
```


### Environment variables -- adding reverb:
This is just to demonstrate how environment variables work inside the timeline. The better way of adding reverb demonstrated earlier with mixer channels.
- add to your timeline prep func:
```
SynthDef(\verb, { |out, verbbus, gate = 1, amp = 1|
  var in = In.ar(verbbus, 2);
  var env = Env.adsr(0.01, 0, 1, 1.0).ar(2, gate);
  var verb = NHHall.ar(in) * env;
  Out.ar(out, verb * amp);
}).add;

~verbbus = Bus.audio(s, 2);
```
- and to the cleanup func:
```
~verbbus.free;
```
- save the changes to load the new SynthDef and bus
  - this environment variable is local to this timeline (assuming `useEnvir` box is still checked)
- cmd-t to make a new track
- click in an empty area and drag to select the time around all your Synth clips
- put the mouse over your new track and shift-S to create a new Synth clip that fills the selected time
- double click on it
  - set defName to `'verb'`
  - set addAction to `'addToTail'`
  - click refresh icon next to args to refresh argument names
  - double click on grayed-out "verbbus" to activate it, put `~verbbus`
  - save
    - you should see that verbbus is now set to e.g. `Bus(audio, 4, 2, localhost)`
- click and drag to select all your Synths, bulk edit Synth arguments, and set `verbbus` to `~verbbus`
  - again, you should see that they all have verbbus set to the same bus number
- bulk edit the same synth arguments and set `verbamt` to `1.0`, or to `rrand(0.0, 1.0)`
  - now when you play you will hear they all are affected by the reverb Synth.
- you could now make an envelope to control the amplitude of this reverb, analogous to overall return level.
- you could also make an envelope to control the verbamt of all of the Synths, analogous to send level.
<br />
</details>

<details>
  <summary><strong>Keyboard and mouse actions</strong></summary>
  
## Mouse interaction
- drag middle of clip to move
- drag edges of clip to resize
- double click on clip to open editor window
- right click anywhere to see action menu
- Zooming
  - cmd-scroll zoom horizontally
  - opt-scroll zoom vertically
- Envelope breakpoint editing (cmd-e to toggle this mode)
  - click and drag to move breakpoints or adjust curves
  - shift-click to add breakpoint
  - opt-click to remvove breakpoint
- Selecting
  - click and drag to select both time and clips
    - hold cmd to just select clips
    - hold opt to just select time
  - hold shift to add/remove clips from existing selection
- click and drag tracks to rearrange

## Key commands
- space toggles play
- opt-s toggles snap to grid
- Navigation
  - enter goes to beginning of timeline
  - [ and ] go to next/previous clip edge on track under mouse
- Editing clip
  - m mutes clip at mouse pointer
  - s splits clip at mouse pointer
  - delete deletes clip at mouse pointer
  - e opens edit window for clip at mouse pointer, or init/cleanup func window for a timeline clip
  - cmd-e toggles mouse editing of envelope breakpoints
- Insert clip
  - C inserts comment clip at mouse
  - S inserts synth clip at mouse
  - P inserts pattern clip at mouse
  - R inserts routine clip at mouse
  - E inserts env clip at mouse
  - T inserts timeline clip at mouse
- Tracks
  - cmd-t inserts new track after track at mouse
  - cmd-T inserts new track before track at mouse
  - cmd-delete deletes track at mouse
- Seletion
  - cmd-a select all clips
  - cmd-i inserts selected time
  - shift-cmd-delete deletes selected time
- cmd-z undo
- cmd-Z redo
- cmd-n new
- cmd-s save as
- cmd-o open
</details>

<br />
<br />
If you do try it out, I would love to know your thoughts, ideas, critiques, and if you find bugs etc please report them on the github issue page with steps to reproduce.
