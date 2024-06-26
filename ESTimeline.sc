ESTimeline {
  var <tracks, <tempo, <>prepFunc, <>cleanupFunc, <>bootOnPrep, <>useEnvir, <>optimizeView;
  var <isPlaying = false;
  var <playbar = 0.0;
  var playBeats, playStartTime, <playClock;
  var dependantFunc;
  var <>undoStack, <>redoStack, <>currentState;
  var <envir;
  var <>parentClip;
  var clock; // this specifically refers to the internal clock to this specific timeline

  //tempo { ^clock.tempo; }
  tempo_ { |val| tempo = val; if (clock.notNil) { clock.tempo_(val) }; this.changed(\tempo, val); }
  tempoBPM { ^tempo * 60 }
  tempoBPM_ { |val| this.tempo_(val / 60); }

  storeArgs { ^[tracks, this.tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView] }

  *new { |tracks, tempo = 1, prepFunc, cleanupFunc, bootOnPrep = true, useEnvir = true, optimizeView = false|
    //var clock = TempoClock(tempo).permanent_(true);

    tracks = tracks ?? [ESTrack()];
    ^super.newCopyArgs(tracks, tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView).initEnvir.initDependantFunc.init(true);
  }

  initEnvir {
    envir = Environment.make {
      ~timeline = this;
    };
  }

  initDependantFunc {
    dependantFunc = { |theTrack, what, value|
      this.changed(\track, [tracks.indexOf(theTrack), theTrack, what, value].flat);
    };
  }

  init { |resetUndo = false, cleanupFirst = false|
    if (cleanupFirst) {
      this.cleanup;
    };

    // forward changed messages from clips
    tracks.do { |track, i|
      track.removeDependant(dependantFunc);
      track.addDependant(dependantFunc);
      track.timeline = this;
    };
    if (resetUndo) {
      undoStack = [];
      redoStack = [];
      currentState = this.asUndoPoint;
    };

    if (bootOnPrep) {
      Server.default.waitForBoot {
        this.prep;
        Server.default.sync;
        this.changed(\init);
      };
    } {
      this.prep;
      this.changed(\init);
    };
  }

  soundingNow {
    if (isPlaying) {
      ^max(playbar, this.now - (playClock.tempo * Server.default.latency));
    } {
      ^this.now;
    }
  }

  now {
    if (isPlaying) {
      ^(playClock.beats - playBeats) + playStartTime;
    } {
      ^playbar;
    };
  }

  now_ { |val|
    playbar = max(val, 0);
    this.changed(\playbar);
  }

  stop { |hard = false|
    if (useEnvir) {
      envir.use { tracks.do(_.stop(hard)); };
    } {
      tracks.do(_.stop(hard));
    };

    //if (clock.notNil) { clock.stop; clock = nil };

    //playbar = this.now;
    {
      if (hard.not) {
        (Server.default.latency * playClock.tempo).wait;
      };
      isPlaying = false;
      this.changed(\isPlaying, false);
    }.fork(playClock);
  }

  prMakeClock { |altClock|
    // stop if playing
    if (isPlaying) { this.stop };

    if (clock.notNil) { clock.stop; clock = nil };

    if (parentClip.notNil and: { parentClip.useParentClock }) {
      playClock = parentClip.track.timeline.playClock;
    } {
      playClock = altClock ?? { clock = TempoClock(tempo); };
    };

    ^playClock;
  }

  play { |startTime, altClock, makeClock = true|
    if (makeClock) {
      this.prMakeClock(altClock);
    };

    if (startTime.notNil) {
      playbar = startTime;
    };

    // save the starting conditions
    //playClock = clock;
    playBeats = playClock.beats;
    playStartTime = playbar;

    if (useEnvir) {
      envir.use {
        tracks.do(_.play(playbar, playClock));
      };
    } {
      tracks.do(_.play(playbar, playClock));
    };


    isPlaying = true;
    this.changed(\isPlaying, true);
  }

  togglePlay {
    if (this.isPlaying) { this.stop(true) } { this.play }
  }

  addTrack { |index, track|
    index = index ?? tracks.size;
    track = track ?? { ESTrack([]) };
    track.addDependant(dependantFunc);
    track.timeline = this;
    tracks = tracks.insert(index, track);
    this.changed(\tracks);
  }

  removeTrack { |index|
    var track = tracks.removeAt(index);
    track.removeDependant(dependantFunc);
    this.changed(\tracks);
  }

  asUndoPoint { ^this.storeArgs.asESArray }

  addUndoPoint {
    var undoPoint = this.asUndoPoint;
    if (undoPoint != currentState) {
      undoStack = undoStack.add(currentState);
      currentState = undoPoint;
      redoStack = [];
    };
  }

  restoreUndoPoint { |undoPoint, clearUndoStack = false, legacy = false|
    var thisTempo;
    if (legacy) {
      currentState = undoPoint.interpret.asESArray;
    };
    currentState = undoPoint;
    {
      this.prFree;
      Server.default.sync;
      #tracks, thisTempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView = Object.fromESArray(currentState);
      this.tempo = thisTempo;
      if (clearUndoStack) {
        undoStack = [];
        redoStack = [];
      };
      this.init;
      this.changed(\restoreUndoPoint);
    }.fork(AppClock)
  }

  undo {
    if (undoStack.size > 0) {
      redoStack = redoStack.add(this.asUndoPoint);
      this.restoreUndoPoint(undoStack.pop);
    };
  }

  redo {
    if (redoStack.size > 0) {
      undoStack = undoStack.add(this.asUndoPoint);
      this.restoreUndoPoint(redoStack.pop);
    };
  }

  prep {
    if (useEnvir) {
      envir.use { this.prepFunc.(); };
    } {
      this.prepFunc.();
    };
    this.clips.do { |clip|
      clip.prep;
    };
  }

  cleanup {
    if (useEnvir) {
      envir.use { this.cleanupFunc.(); }
    } {
      this.cleanupFunc.();
    };
    this.clips.do { |clip|
      clip.cleanup;
    };
  }

  prFree {
    this.cleanup;
    tracks.do(_.free);
  }

  free {
    this.prFree;
    this.changed(\free);
    this.release;
  }

  encapsulateSelf {
    var duration = this.duration;
    if (duration > 0) {
      var newTimeline = ESTimeline().restoreUndoPoint(currentState);
      this.prFree;
      tracks = [ESTrack([ESTimelineClip(0, duration, timeline: newTimeline)])];
      prepFunc = {};
      cleanupFunc = {};
      this.init;
      this.changed(\encapsulateSelf);
    }
  }

  hasSolo {
    tracks.do { |track|
      if (track.solo) { ^true }
    };
    ^false;
  }

  duration {
    ^this.clips.collect(_.endTime).maxItem ?? 0
  }

  clipsInRange { |trackAIndex, trackBIndex, timeA, timeB|
    var startTrackIndex, endTrackIndex;
    var ret = [];
    #startTrackIndex, endTrackIndex = [trackAIndex, trackBIndex].sort;
    tracks[startTrackIndex..endTrackIndex].do { |track|
      ret = ret ++ track.clipsInRange(timeA, timeB);
    };
    ^ret;
  }

  clips {
    ^tracks.collect(_.clips).flat;
  }

  at { |name|
    var contenders = [];
    this.clips.do { |clip|
      if (clip.name == name) {
        contenders = contenders.add(clip);
      };
    };
    if (contenders.size == 0) { ^nil };
    contenders.sort({ |a, b| (a.startTime + (a.duration / 2) - this.now).abs < (b.startTime + (b.duration / 2) - this.now).abs });
    ^contenders[0];
  }

  insertTime { |timeA, timeB|
    tracks.do(_.insertTime(timeA, timeB));
  }

  deleteTime { |timeA, timeB|
    tracks.do(_.deleteTime(timeA, timeB));
  }
}

