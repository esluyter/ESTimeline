# ESTimeline

The distant goal is that anything you can do in SuperCollider could be sequenced on a visual and editable timeline...

Note that this is a work in progress and all is subject to revision.

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
  - for the moment just real-time but some of this could be translated easily to work NRT
    - there are certain things impossible in NRT, i.e. to do with real-time input
  - the basic goal is only to "execute this code at this particular time"
    - although the competing goal is to make it easy to do the things you want to do, which is subjective
  - as little architecture as possible is forced on you,
    - possible to disable timeline-specific clock and environment so as to interact with the timeline as part of a larger project
    - I am building in optional use of ddwMixerChannel, but
      - ddwMixerChannel is not required
      - possible to play clips with any bus, target, addAction, etc. for full flexibility
- **DAW-like GUI** for editing and playback
  - Keyboard and mouse interface to full extent of Timeline capabilities, with built-in code editing
  - Snap to grid optional
  - Gray playhead is "scheduling playhead" and black playhead is "sounding playhead" -- to take into account server latency.
    - Routines can be played with additional latency so non-sounding events line up with the sounding playhead. The goal is an accurate visual representation of what you are hearing / when the code is executed.
  - If using ddwMixerChannel, there is a GUI mixer window with track insert FX, pre fade sends and post fade sends.
- **Non-linear:** "goto" command to jump to a clip or a point in time enabling complex real-time behaviors (variable-length looping, conditional branching...)
- **Tracks** can contain all clip types
  - tracks can be muted/soloed and rearranged
  - individual clips can be muted
  - if using ddwMixerChannel, tracks will play on a mixer channel specified by the track's name
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

## Hypothetical features
- Time features
  - Clock follow: e.g. sync up with an Ableton timeline or midi show control
- Track/clips
  - More clip types (e.g. OSCdef, loop, audio file, midi/piano/drum roll, VST)
  - Reference clips to create clones that all change together
- Envelopes
  - Ability to draw freehand with mouse 
  - More live interaction - e.g. map a controller to a bus and record its movements to an envelope
  - Higher dimensional envelopes - e.g. movement through x/y space
  - Timeline tempo envelopes (this is already possible but kind of annoying, using an Env clip and a Routine clip)
  - Automate mixer channel parameters (level, pan, sends, insert fx parameters) with envelopes
- Playback and record audio files
  - easily access this Buffer for further manipulation
- MIDI integration
  - not sure how DAW-like I want to make this.....
- Library integration
  - ddwPlug -- simplify bus routing for modulation
  - VSTPlugin, somehow..... this could be a can of worms
  - clothesline -- put whole .scd files on the timeline

## Issues
1. Although I've tried to make it pleasant, the GUI based code editing environment does not syntax highlight, autocomplete, etc -- for this reason I've added "Open in IDE" / "Copy from IDE" buttons as necessary.
    - Solution would be to someday add a Qt code view to core SC
2. When there are lots of quick zig-zags, high-resolution envelope drawing makes the GUI freeze up
    - to avoid this I have extremely pixelated the envelope drawing when zoomed in. Solution would be to someday add a better Qt envelope view to core sc.
3. There is a limit to the complexity of a timeline created using SCLang (i.e. by evaluating `ESTimeline([ESTrack([....`) -- it may only contain max 256 functions.
    - to avoid this I have created a light custom file format that compiles complex timeline structures from the inside out

<br />
</details>

<details>
  <summary><strong>Getting started: installing and tutorial</strong></summary>
  
## Installing
Download or clone this repository into your SuperCollider Extensions directory. To see where this is, go to `File > Open user support directory` and find the `Extensions` directory, or evaluate:
```
Platform.userExtensionDir
```

## Tutorial: basic workflow examples
```
(
~timeline = ESTimeline(bootOnPrep: true);
~window = ESTimelineWindow(timeline: ~timeline);
)
```
- this boots the default server. You can make it not do this by setting `bootOnPrep` to false or going into "Prep / Cleanup funcs" and unchecking `bootOnPrep`.

### SynthDefs:
- put your SynthDef in the timeline's prep function (click the "edit prep/cleanup funcs" button) e.g.
```
SynthDef(\sin, { |out, freq = 200, gate = 1, amp = 0.1, preamp = 1.5, attack = 0.001, release = 0.01, pan, verbbus, verbamt, vibrato = 0.2|
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
- hit save when you're done to save the prepFunc and load it.

### Making tracks:
- press cmd-t to add a track after the one your mouse is currently over, or shift-cmd-T to add it before the current track
- cmd-delete deletes a track
- each track can contain any kind of clip in any combination
- mute and solo tracks using the buttons on the left panel
- click and drag in the left panel to rearrange tracks

### Synth Clips:
- create a bunch of Synth clips (point the mouse where you want it and press shift-S, or use right click menu)
  - drag them around to move them
  - drag their edges to resize them (a red bar appears when you are within the resize zone)
  - option-drag to copy a clip
  - check the `snapToGrid` box or press opt-s to align your edits with the tempo grid
  - double-click on a clip to edit it
    - double-click on the grayed out `freq` parameter to activate it, then you can set it to any valid SuperCollider expression, like `220`
    - press save when you're done
  - if you play now by clicking to place the playhead and pressing space, you will hear they play the default synth
    - press space again to stop playback

### Bulk edit synth clips:
- click in an empty area and drag to select all the Synth clips (they will be highlighted in cyan when selected)
- right click, "clip actions > synth actions > bulk edit synth defName"
  - and set them to `'sin'`.
  - play again and you hear they now all play your SynthDef
  - double-click in an empty area to remove selection

### Scrolling and zooming:
- use trackpad to scroll left and right or click and drag ruler at top
- cmd-scroll to zoom in and out horizontally
- opt-scroll to zoom in and out vertically (when there are more than one track)

### Envelopes for Synth parameters:
- right click a Synth clip, "clip actions > synth actions > add env for synth argument"
- pick "freq" from the list and hit OK
  - this will by default add a new track with an envelope clip on it that is the length of your Synth clip, with a unique name (starting from 'freq0'), and it will update the freq argument of the Synth clip to read from this envelope's bus

### Editing Envelopes:
- cmd-e to enter envelope breakpoint editor mode
  - click and drag to move the breakpoints around or adjust curves,
  - shift-click to add breakpoints,
  - option-click to remove them
- by default, these envelopes will map to the range of the parameter name .asSpec
  - to rescale, right click, clip actions > "set env range keeping breakpoint values"
- hit cmd-e again to leave envelope breakpoint editor mode

### Bulk edit Synths -- To make this envelope affect all your Synths:
- drag the edges of the envelope clip to resize it, so that it covers the entire range of your Synth clips
- click and drag to select all the Synth clips (your envelope clip can also be selected, it doesn't matter)
- right click, "clip actions > synth actions > bulk edit synth arguments"
- assign the `freq` of all the clips to 
`\freq0`
(or whatever the name of the envelope clip is)
- you should see all their freqs change to `a4` -- this is the audio rate bus that the Env clip has created for you (you can override this behavior)
- cmd-e to edit the breakpoints again
- you should hear it is now controlling all the synths' pitches
- make sure you've left breakpoint edit mode when you want to move clips around, and double-click in an empty area to remove the selection.

### Bulk edit Synths -- Random panning:
- Select all your Synth clips
- right click > clip actions > synth actions > bulk edit synth arguments
- for `pan` put in `rrand(-1.0, 1.0)` and check the "hard coded" box
  - this will generate a random hard-coded pan per clip. (if you want it to be newly random every time you play it, uncheck the box)

### Environment variables -- adding reverb:
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

### Pattern Clips:
- make a new track and shift-P to make a pattern clip
- double click to edit, e.g.:
```
Pbind(
  \instrument, \sin,
  \verbbus, ~verbbus,
  \verbamt, Pwhite(0.0, 1.0).linexp(0, 1, 0.1, 3.0),
  \degree, Pbrown(0, 7 * 3 + 1, 3),
  \octave, Pdup(Pwhite(1, 10), Pwhite(3, 5)),
  \pan, Pwhite(-1.0, 1.0),
  \dur, Pbrown().linexp(0, 1, 0.02, 1.0)
)
```
- you will hear this uses the same reverb synth
- if you want to try a new random seed, click "re-roll" button and save
  - you can always undo if you don't like it
- you can drag the edges to adjust start and end point without changing the timing of the notes
  - you can split it into two by pointing with the mouse where you want the split and pressing s
- if you make a new track and a new envelope, name the envelope `pan0` and set its range from -1 to 1
- edit the panning to your liking, and update the pattern(s) with
```
  \pan, ~thisTimeline[\pan0],
```
    
### Timeline clips:
- above the main timeline, click "Open as clip in new timeline"
  - Now this little system, the synths, patterns, buses and envelopes, are all encapsulated in this timeline clip, which won't interfere with e.g. another ~verbbus that you happen to use elsewhere.
  - (in fact you can duplicate the timeline clip by option-dragging onto a new track, and the two will play simultanously each using its own environment and bus.)
  - you can also resize the clips, move the mouse cursor over the clip and use the s key to split it into two separate timeline clips, etc.

### Using Routine clips:
- shift-R to make a Routine clip, double click to edit
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
- You can think of Routine clips as kind of your generic "execute this code here", and if you want say OSC out to a light board to line up with the sounding events, check the `addLatency` box.
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
- to jump to a clip named `next`, use
```
~thisTimeline.goto(\next)
```
- you can use a comment clip (shift-C) for this dummy "next" clip -- the first line of the comment is its name
- you can also goto a number, which will be interpreted as beat number.
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

<details>
  <summary><strong>Current ad-hoc mixer interface</strong></summary>

## Current ad-hoc mixer interface
Everything works, and everything saves with timeline except mixer channel output bus (will reset to default)
```
(
OSCdef(\test, { |msg|
  var oscMsg, synthId, busIndex, peaks, powers;
  var index;
  //msg.postln;
  # oscMsg, synthId, busIndex = msg;
  # peaks, powers = msg[3..].clump(2).flop;
  index = ~channelIndexMap[busIndex];
  //[index, peaks, powers].postln;
  defer {
    //[busIndex, index].postln;
    if (index.notNil) {
      peaks.size.do { |i|
        ~peaks[index][i].value = powers[i].ampdb.linlin(-60, 0, 0, 1);
        ~peaks[index][i].peakLevel = peaks[i].ampdb.linlin(-60, 0, 0, 1);
      };
    };
  };
}, '/mixerChannel');

{
  var width = 1500, height = 600;
  var left = Window.availableBounds.width - width;
  if (~mixerWindow.notNil) { ~mixerWindow.close };
  ~mixerWindow = Window("Mixer", Rect(left, 0, width, height)).background_(Color.gray(0.8)).front;
}.value;

~winFunc = {
  var width = 1500, height = 600;
  var left = Window.availableBounds.width - width;
  var top;
  var levelAdjust = 10;
  var meterHeight = 250;
  var dbHeight = 20;
  var panHeight = 30;
  var outHeight = 20;
  var nameHeight = 40;
  var muteHeight = 22;
  var insertHeight = 15;
  var trackWidth = 80;
  var faderSpec = ControlSpec(0.0, 4.0, 4);//ControlSpec(0.0, 2, \amp);
  var panSpec = \pan.asSpec;

  var mixerChannels = ~timeline./*tracks[0].clips[0].timeline.*/orderedMixerChannels; //.postcs;
  var mixerChannelNames = ~timeline.orderedMixerChannelNames;
  // [1, \melody, \harmony, [2, \bass, \kik, \sn, \master], \drums, \fx]
  // mixerChannelNames[i]
  var mcnFunc = { |arr|
    var index = arr[0];
    var ret = [];
    arr[1..].do { |item|
      if (item.isArray.not) {
        ret = ret.add([item, index])
      } {
        ret = ret ++ mcnFunc.(item);
      };
    };
    ret;
  };
  var mixerChannelNamesFlat = mcnFunc.(mixerChannelNames);

  var mcfFunc = { |arr, level = 0|
    var ret = [];
    arr.do { |item|
      if (item.isArray.not) {
        ret = ret.add([item, level])
      } {
        ret = ret ++ mcfFunc.(item, level + 1);
      };
    };
    ret;
  };
  var mixerChannelsFlat = mcfFunc.(mixerChannels);

  // in case of problem just don't throw infinite error messages..
  try {

    ~channelIndexMap = ();
    mixerChannelsFlat.do { |arr, i| var mc = arr[0]; if (mc.notNil) { ~channelIndexMap[mc.inbus.index] = i } }; //ugh why

    ~scrollView.remove;
    ~scrollView = ScrollView(~mixerWindow, Rect(0, 0, width, height)).hasBorder_(false).background_(Color.gray(0.8));

    top = height;

    mixerChannelsFlat.do { |arr, i| var mc = arr[0]; var level = arr[1];
      var bounds = Rect(i * trackWidth + 14, 0, trackWidth - 3, height - 3 - (level * levelAdjust));
      UserView(~scrollView, bounds).background_(Color.gray(0.85));
    };

    //make sure right side margin is drawn
    View(~scrollView, Rect(mixerChannels.size * trackWidth + 14, 0, 11, height));

    // names
    top = top - nameHeight - 5;
    mixerChannelsFlat.do { |arr, i| var mc = arr[0]; var level = arr[1];
      var bounds = Rect(i * trackWidth + 15, top - (level * levelAdjust), trackWidth - 5, 40);
      StaticText(~scrollView, bounds).align_(\center).string_(mc.name).font_(Font.sansSerif(12, true)).stringColor_(Color.gray(0.5)).background_(Color.gray(0.9));
      // draw folder indicators
      if ((i > 0) and: { mixerChannelsFlat[i - 1][1] > level }) {
        UserView(~scrollView, Rect(i * trackWidth + 7 - levelAdjust, top + nameHeight - (mixerChannelsFlat[i - 1][1] * levelAdjust), levelAdjust + 3, levelAdjust)).drawFunc_({ |view|
          Pen.moveTo(0@0);
          Pen.lineTo(levelAdjust@levelAdjust);
          Pen.lineTo(view.bounds.width@levelAdjust);
          Pen.lineTo(view.bounds.width@0);
          Pen.lineTo(0@0);
          Pen.color = Color.gray(0.9);
          Pen.fill;
        })//.background_(Color.red);
      };
    };

    // out bus
    top = top - outHeight - 2.5;
    ~outViews = mixerChannelsFlat.collect { |arr, i| var mc = arr[0]; var level = arr[1];
      var bounds = Rect(i * trackWidth + 15, top - (level * levelAdjust), trackWidth - 5, outHeight);
      PopUpMenu(~scrollView, bounds).items_(BusDict.menuItems(Server.default)).font_(Font.sansSerif(10, true)).background_(Color.gray(0.65)).stringColor_(Color.gray(0.95)).action_({ |view|
        mc.outbus = view.value;
      }).value_(mc.outbus.index);
    };

    // mute/record
    top = top - muteHeight -3;
    mixerChannelsFlat.do { |arr, i| var mc = arr[0]; var level = arr[1];
      [
        Button(~scrollView, Rect(i * trackWidth + 25, top - (level * levelAdjust), muteHeight, muteHeight)).states_([
          ["⚫︎" /*◉︎*/, Color.gray(0.6), Color.gray(0.8)],
          ["⚫︎", Color.red, Color.black]])
        .focusColor_(Color.clear).font_(Font.sansSerif(16, true)).action_({ |view|
          if (view.value.asBoolean) {
            mc.startRecord;
            if (~timeline.isPlaying.not) {
              ~timeline.play;
            };
          } {
            mc.stopRecord;
          };
        }).value_(mc.isRecording),
        Button(~scrollView, Rect(i * trackWidth + 58, top - (level * levelAdjust), muteHeight, muteHeight)).states_([
          ["M", Color.gray(0.55), Color.gray(0.8)],
          ["M", Color.gray(0.7), Color.gray(0.3)]])
        .focusColor_(Color.clear).font_(Font.sansSerif(16, true)).action_({ |view|
          mc.mute(view.value.asBoolean);
        }).value_(mc.muted),
      ]
    };

    // fader
    top = top - meterHeight - 5;
    ~peaks = mixerChannelsFlat.collect { |arr, i| var mc = arr[0]; var level = arr[1];
      var levelWidth = 35 / mc.inChannels;
      mc.inChannels.collect { |j|
        LevelIndicator(~scrollView, Rect((i * trackWidth) + (j * levelWidth) + 20, top, levelWidth - 3, meterHeight - (level * levelAdjust)) ).warning_(0.9).critical_(0.99)
        .drawsPeak_(true)
        .numTicks_(0)
        .numMajorTicks_(0)
        .meterColor_(Color.hsv(0.3, 0.7, 0.99))
        .warningColor_(Color.hsv(0.15, 0.6, 1))
        .background_(Color.gray(0.6));
      };
    };

    ~sliders = mixerChannelsFlat.collect { |arr, i| var mc = arr[0]; var level = arr[1];
      var thisMeterHeight = meterHeight - (level * levelAdjust);
      var bounds = Rect((i * trackWidth) + 55, top - 1, 30, thisMeterHeight + 2);
      var slider = Slider(~scrollView, bounds).background_(Color.gray(0.8)).value_(faderSpec.unmap(mc.level)).action_({ |view|
        //mc.level = faderSpec.map(view.value);
        ESTimeline.at(mixerChannelNamesFlat[i][1]).setMixerChannel(mixerChannelNamesFlat[i][0], \level, faderSpec.map(view.value));
      }).mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
        if (clickCount > 1) {
          ESTimeline.at(mixerChannelNamesFlat[i][1]).setMixerChannel(mixerChannelNamesFlat[i][0], \level, 1);
          true;
        } { false }
      });

      UserView(~scrollView, bounds).drawFunc_({
        var color = Color.gray(0.5);
        Pen.stringAtPoint("+12", 5@5, Font.sansSerif(10), color);
        Pen.stringAtPoint("+6", 5@((1 - faderSpec.unmap(2)) * thisMeterHeight + 4), Font.sansSerif(10), color);
        Pen.stringAtPoint("0", 5@((1 - faderSpec.unmap(1)) * thisMeterHeight), Font.sansSerif(10), color);
        Pen.stringAtPoint("-6", 5@((1 - faderSpec.unmap(0.5)) * thisMeterHeight - 4), Font.sansSerif(10), color);
        Pen.stringAtPoint("-12", 5@((1 - faderSpec.unmap(0.25)) * thisMeterHeight - 6), Font.sansSerif(10), color);
        Pen.stringAtPoint("-20", 5@((1 - faderSpec.unmap(0.1)) * thisMeterHeight - 8), Font.sansSerif(10), color);
        Pen.stringAtPoint("-inf", 5@(thisMeterHeight - 12), Font.sansSerif(10), color);
      }).acceptsMouse_(false);
      slider;
    };

    top = top - dbHeight - 2;
    ~dbViews = mixerChannelsFlat.collect { |arr, i| var mc = arr[0];
      var bounds = Rect(i * trackWidth + 20, top, trackWidth - 15, dbHeight);
      NumberBox(~scrollView, bounds).background_(Color.gray(0.85)).normalColor_(Color.gray(0.4)).font_(Font.sansSerif(11)).value_(mc.level.ampdb).align_(\center).action_({ |view|
        //mc.level = view.value.dbamp
        ESTimeline.at(mixerChannelNamesFlat[i][1]).setMixerChannel(mixerChannelNamesFlat[i][0], \level, view.value.dbamp);
      }).scroll_step_(0.05).shift_scale_(5).ctrl_scale_(2.5);
    };

    top = top - panHeight - 5;
    ~panViews = mixerChannelsFlat.collect { |arr, i| var mc = arr[0];
      var bounds = Rect(i * trackWidth + 55, top, panHeight, panHeight);
      var panString = (mc.pan.abs * 100).asInteger.asString ++ " " ++ if (mc.pan.isPositive) { "R" } { "L" };
      if (mc.pan == 0) { panString = "C" };
      [
        Knob(~scrollView, bounds).value_(panSpec.unmap(mc.pan)).centered_(true).mode_(\vert).step_(0.0025).action_({ |view|
          //mc.pan = panSpec.map(view.value);
          ESTimeline.at(mixerChannelNamesFlat[i][1]).setMixerChannel(mixerChannelNamesFlat[i][0], \pan, panSpec.map(view.value));
        }).mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
          if (clickCount > 1) { ESTimeline.at(mixerChannelNamesFlat[i][1]).setMixerChannel(mixerChannelNamesFlat[i][0], \pan, 0); true } { nil };
        }),
        StaticText(~scrollView, bounds.copy.left_(i * trackWidth + 20, top, 30, panHeight)).align_(\right).string_(panString).font_(Font.sansSerif(10, true)).stringColor_(Color.gray(0.5));
      ];
    };



    // inserts
    ~insertScrollViews = [];
    ~insertUserViews = [];
    mixerChannelNamesFlat.do { |arr, i| var name = arr[0]; var id = arr[1];
      var insertView = ScrollView(~scrollView, Rect(i * trackWidth + 20, 5, trackWidth - 15, top - 15)).hasBorder_(false);
      var timeline = ESTimeline.at(id);
      var template = timeline.mixerChannelTemplates[name];
      var funcViewFactory = { |bounds, index|
        StaticText(insertView, bounds)
        .string_(" { }")
        .background_(Color.gray(0.6))
        .font_(Font.monospace(9))
        .stringColor_(Color.gray(0.8))
        .mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
          if (clickCount > 1) {
            ESBulkEditWindow.code("Insert FX:", template.fx[index].asCompileString, { |string|
              var func = string.interpret;
              if (func.isNil) {
                ESBulkEditWindow.ok;
              } {
                template.fx[index] = func;
              };
            });
          };
        }).setContextMenuActions(
          MenuAction("Delete", {
            template.fx.removeAt(index);
            ~winFunc.value;
          })
        );
      };
      var sendViewFactory = { |bounds, index, method, stringColor, dbColor, barColor|
        var send = template.perform(method)[index];
        var clickPoint, clickVal;
        UserView(insertView, bounds).background_(Color.gray(0.75)).drawFunc_({ |view|
          var levelPx = faderSpec.unmap(send[1]) * view.bounds.width;
          var dbString = send[1].ampdb.round(0.1).asString;
          var dbStringWidth = QtGUI.stringBounds(dbString, Font.sansSerif(8)).width + 2;
          Pen.addRect(Rect(0, 0, levelPx, view.bounds.height));
          Pen.color = barColor;
          Pen.fill;
          Pen.stringAtPoint(send[0].asCompileString, 2@2, Font.monospace(9), stringColor);
          Pen.stringAtPoint(dbString, (view.bounds.width - dbStringWidth)@4, Font.sansSerif(8), dbColor);
        }).mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
          if (clickCount > 1) {
            var wasPre = method == 'preSends';
            ESBulkEditWindow.keyValue("Edit Send:", "name", send[0].asCompileString, "db", send[1].ampdb.asCompileString, "pre fade", wasPre, callback: { |name, level, pre|
              var arr = [name.interpret, level.interpret.dbamp];
              if (pre) {
                if (wasPre) {
                  template.preSends[index] = arr;
                } {
                  template.postSends.removeAt(index);
                  template.preSends = template.preSends.add(arr);
                };
              } {
                if (wasPre) {
                  template.preSends.removeAt(index);
                  template.postSends = template.postSends.add(arr);
                } {
                  template.postSends[index] = arr;
                }
              };
              timeline.initMixerChannels;
            });
          } {
            clickPoint = x@y;
            clickVal = template.perform(method)[index][1];
          };
        }).mouseMoveAction_({ |view, x, y, mods|
          var yDelta = clickPoint.y - y;
          var step = 0.005;
          var val;
          if (mods.isAlt) {
            step = step * 0.2;
          };
          if (mods.isCmd) {
            step = step * 2;
          };
          val = faderSpec.map(faderSpec.unmap(clickVal) + (yDelta * step));
          template.perform(method)[index][1] = val;
          timeline.mixerChannels[name].perform(method)[index].level = val;
          view.refresh;
        }).setContextMenuActions(
          MenuAction("Delete", {
            template.perform(method).removeAt(index);
            ~winFunc.value;
          })
        );
      };
      var userViews = [];

      max(((top - 20) / (insertHeight + 5)).asInteger, template.fx.size + template.preSends.size + template.postSends.size).do { |j|
        var bounds = Rect(0, j * (insertHeight + 5) + 10, trackWidth - 15, insertHeight);
        if (j < template.fx.size) {
          userViews = userViews.add(funcViewFactory.(bounds, j));
        } {
          if (j < (template.fx.size + template.preSends.size)) {
            var index = j - template.fx.size;
            userViews = userViews.add(sendViewFactory.(bounds, index, 'preSends', Color.gray(0.9), Color.white, Color.gray(0.5)));
          } {
            if (j < (template.fx.size + template.preSends.size + template.postSends.size)) {
              var index = j - template.fx.size - template.preSends.size;
              userViews = userViews.add(sendViewFactory.(bounds, index, 'postSends', Color.gray(0.3), Color.gray(0.5), Color.white));
            } {
              UserView(insertView, bounds)
              .background_(Color.gray(0.82)).setContextMenuActions(
                MenuAction("New Insert Func", {
                  ESBulkEditWindow.code("New Insert FX:", "{ |out|\n  var sig = In.ar(out, " ++ template.inChannels ++ ");\n  sig;\n}", { |string|
                    var func = string.interpret;
                    if (func.isNil) {
                      ESBulkEditWindow.ok;
                    } {
                      template.fx = template.fx.add(func);
                      ~winFunc.value;
                    };
                  });
                }),
                MenuAction("New Send", {
                  ESBulkEditWindow.keyValue("New Send:", "name", "'verb'", "db", 0.0, "pre fade", callback: { |name, level, pre|
                    if (pre) {
                      template.preSends = template.preSends.add([name.interpret, level.interpret.dbamp]);
                    } {
                      template.postSends = template.postSends.add([name.interpret, level.interpret.dbamp]);
                    };
                    timeline.initMixerChannels;
                  });
                }),
              );
            };
          };
        };
      };

      ~insertScrollViews = ~insertScrollViews.add(insertView);
      ~insertUserViews = ~insertUserViews.add(userViews);
    };



    // this is leaky when MC's are freed
    // FIXED I think
    // always call .release.free on a mixerChannel
    // e.g. mixerChannels.do(_.release.free)

    // and settings reset...
    // make wrapper class for this
    mixerChannelsFlat.do { |arr| var mc = arr[0];
      mc.removeDependant(~dependantFunc);
    };
    ~dependantFunc = { |mc, what, args|
      var i = ~channelIndexMap[mc.inbus.index];
      if (i.notNil) {
        //[i, what, args].postln;
        if (what[\what] == \control) {
          if (what[\name] == \level) {
            ~sliders[i].value = faderSpec.unmap(mc.level);
            ~dbViews[i].value = mc.level.ampdb;
          };
          if (what[\name] == \pan) {
            var panString = (mc.pan.abs * 100).asInteger.asString ++ " " ++ if (mc.pan.isPositive) { "R" } { "L" };
            if (mc.pan == 0) { panString = "C" };
            ~panViews[i][0].value = panSpec.unmap(mc.pan);
            ~panViews[i][1].string_(panString);
          }
        };
      };
    };
    mixerChannelsFlat.do { |arr| var mc = arr[0];
      mc.addDependant(~dependantFunc);
    };
  }; // end try
};

~timeline.removeDependant(~timelineDependantFunc);
~timelineDependantFunc = { |self, what, args|
  //[what, args].postln;
  //timelineView.refresh;
  defer {
    switch (what)
    { \initMixerChannels } {
      ~winFunc.value;
    }
    { \playbar } {
      ~winFunc.value;
    }
    { \track } {
      if (args.indexOf(\initMixerChannels).notNil) {
        ~winFunc.value;
      }
    }
    { \isPlaying } {
      var names;

      if (~timeline.isPlaying) {
        var waitTime = 5.reciprocal; // 5 fps refresh mixer
        names = ~timeline.orderedMixerChannelNames;
        ~mixerRout.stop; // just to make sure
        ~mixerRout = {
          inf.do { |i|
            var nowNames = ~timeline.orderedMixerChannelNames;
            if (nowNames != names) {
              names = nowNames;
              ~winFunc.value;
            };
            waitTime.wait;
          };
        }.fork(AppClock) // lower priority clock for GUI updates
      } {
        ~mixerRout.stop;
        ~winFunc.value;
      };
    }
  }
};
~timeline.addDependant(~timelineDependantFunc);


~winFunc.value;
)
```
</details>

<br />
<br />
If you do try it out, I would love to know your thoughts, ideas, critiques, and if you find bugs etc please report them on the github issue page with steps to reproduce.
