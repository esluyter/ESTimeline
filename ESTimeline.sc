ESTimeline {
  var <tracks, <tempo, <>initFunc, <>cleanupFunc, <>bootOnInit, <>useEnvir, <>optimizeView;
  var <isPlaying = false;
  var <playbar = 0.0;
  var playBeats, playStartTime, playClock;
  var dependantFunc;
  var <>undoStack, <>redoStack, <>currentState;
  var <envir;
  var <>parentClip;
  var <clock;

  //tempo { ^clock.tempo; }
  tempo_ { |val| tempo = val; if (clock.notNil) { clock.tempo_(val) }; this.changed(\tempo, val); }
  tempoBPM { ^tempo * 60 }
  tempoBPM_ { |val| this.tempo_(val / 60); }

  storeArgs { ^[tracks, this.tempo, initFunc, cleanupFunc, bootOnInit, useEnvir, optimizeView] }

  *new { |tracks, tempo = 1, initFunc, cleanupFunc, bootOnInit = true, useEnvir = true, optimizeView = false|
    //var clock = TempoClock(tempo).permanent_(true);

    tracks = tracks ?? [ESTrack()];
    ^super.newCopyArgs(tracks, tempo, initFunc, cleanupFunc, bootOnInit, useEnvir, optimizeView).initEnvir.initDependantFunc.init(true);
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

    if (bootOnInit) {
      Server.default.waitForBoot {
        this.doInitFunc;
        Server.default.sync;
        this.changed(\init);
      };
    } {
      this.doInitFunc;
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

  stop {
    if (useEnvir) {
      envir.use { tracks.do(_.stop); };
    } {
      tracks.do(_.stop);
    };

    //if (clock.notNil) { clock.stop; clock = nil };

    //playbar = this.now;
    isPlaying = false;
    this.changed(\isPlaying, false);
  }

  prMakeClock { |altClock|
    // stop if playing
    if (isPlaying) { this.stop };
    isPlaying = true;
    this.changed(\isPlaying, true);

    if (clock.notNil) { clock.stop; clock = nil };

    if (parentClip.notNil and: { parentClip.useParentClock }) {
      playClock = parentClip.track.timeline.clock;
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
    }
  }

  togglePlay {
    if (this.isPlaying) { this.stop } { this.play }
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

  asUndoPoint { ^this.storeArgs.asCompileString }

  addUndoPoint {
    var undoPoint = this.asUndoPoint;
    if (undoPoint != currentState) {
      undoStack = undoStack.add(currentState);
      currentState = undoPoint;
      redoStack = [];
    };
  }

  restoreUndoPoint { |undoPoint, clearUndoStack = false|
    var thisTempo;
    currentState = undoPoint;
    {
      this.prFree;
      Server.default.sync;
      #tracks, thisTempo, initFunc, cleanupFunc, bootOnInit, useEnvir, optimizeView = currentState.interpret;
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

  doInitFunc {
    if (useEnvir) {
      envir.use { this.initFunc.(); };
    } {
      this.initFunc.();
    };
  }

  cleanup {
    if (useEnvir) {
      envir.use { this.cleanupFunc.(); }
    } {
      this.cleanupFunc.();
    }
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

  initFuncString {
    if (initFunc.isFunction) {
      var cs = initFunc.asCompileString;
      if (cs.size == 2) { ^"" };
      ^cs[1..cs.size-2];
    };
    ^"";
  }

  cleanupFuncString {
    if (cleanupFunc.isFunction) {
      var cs = cleanupFunc.asCompileString;
      if (cs.size == 2) { ^"" };
      ^cs[1..cs.size-2];
    };
    ^"";
  }

  hasSolo {
    tracks.do { |track|
      if (track.solo) { ^true }
    };
    ^false;
  }

  duration {
    ^tracks.collect(_.clips).flat.collect(_.endTime).maxItem ?? 0
  }
}

