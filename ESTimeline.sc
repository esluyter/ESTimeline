ESTimeline {
  var <tracks, <clock, <>initFunc, <>cleanupFunc, <>bootOnInit, <>useEnvir, <>optimizeView;
  var <isPlaying = false;
  var <playbar = 0.0;
  var playBeats, playStartTime, playClock;
  var dependantFunc;
  var <>undoStack, <>redoStack, <>currentState;
  var <envir;

  storeArgs { ^[tracks, this.tempo, initFunc, cleanupFunc, bootOnInit, useEnvir, optimizeView] }

  *new { |tracks, tempo = 1, initFunc, cleanupFunc, bootOnInit = true, useEnvir = true, optimizeView = false|
    var clock = TempoClock(tempo).permanent_(true);
    ^super.newCopyArgs(tracks, clock, initFunc, cleanupFunc, bootOnInit, useEnvir, optimizeView).initEnvir.initDependantFunc.init(true);
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

    //playbar = this.now;
    isPlaying = false;
    this.changed(\isPlaying, false);
  }

  play { |startTime, altClock|
    // stop if playing
    if (isPlaying) { this.stop };
    isPlaying = true;
    this.changed(\isPlaying, true);

    if (startTime.notNil) {
      playbar = startTime;
    };


    // override default clock
    playClock = altClock ?? clock;

    // save the starting conditions
    playClock = clock;
    playBeats = clock.beats;
    playStartTime = playbar;

    if (useEnvir) {
      envir.use {
        tracks.do(_.play(playbar, clock));
      };
    } {
      tracks.do(_.play(playbar, clock));
    }
  }

  togglePlay {
    if (this.isPlaying) { this.stop } { this.play }
  }

  tempo {
    ^clock.tempo;
  }

  tempo_ { |val|
    clock.tempo_(val);
    this.changed(\tempo, val);
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

  restoreUndoPoint { |undoPoint|
    var tempo;
    currentState = undoPoint;
    this.prFree;
    #tracks, tempo, initFunc, cleanupFunc, bootOnInit = currentState.interpret;
    clock = TempoClock(tempo).permanent_(true);
    this.init;
    this.changed(\restoreUndoPoint);
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
    clock.stop;// why is this making newly created timelines crash bc their clock is stopped?
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
    ^tracks.collect(_.clips).flat.collect(_.endTime).maxItem
  }
}

