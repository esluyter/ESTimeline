ESTimeline {
  var <tracks, <tempo, <>prepFunc, <>cleanupFunc, <>bootOnPrep, <>useEnvir, <>optimizeView, <gridDivision, <snapToGrid;
  var <isPlaying = false;
  var <playbar = 0.0;
  var playBeats, playStartTime, <playClock;
  var dependantFunc;
  var <>undoStack, <>redoStack, <>currentState;
  var <envir;
  var <>parentClip;
  var clock; // this specifically refers to the internal clock to this specific timeline

  //tempo { ^clock.tempo; }
  tempo_ { |val|
    tempo = val;
    if (clock.notNil) {
      clock.tempo_(val);
      this.currentClips.do(_.prTempoChanged(val));
    };
    this.changed(\tempo, val);
  }
  tempoBPM { ^tempo * 60 }
  tempoBPM_ { |val| this.tempo_(val / 60); }
  gridDivision_ { |val| gridDivision = val; this.changed(\gridDivision); }
  snapToGrid_ { |val| snapToGrid = val; this.changed(\snapToGrid); }
  tracks_ { |val|
    this.prFree;
    tracks = val;
    this.init;
    this.changed(\tracks);
  }

  storeArgs { ^[tracks, this.tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, gridDivision, snapToGrid] }
  defaultUndoPoint { ^[[ESTrack([])], 1, nil, nil, true, true, false, 4, false].asESArray }

  *new { |tracks, tempo = 1, prepFunc, cleanupFunc, bootOnPrep = true, useEnvir = true, optimizeView = false, gridDivision = 4, snapToGrid = false|
    //var clock = TempoClock(tempo).permanent_(true);

    tracks = tracks ?? [ESTrack()];
    ^super.newCopyArgs(tracks, tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, gridDivision, snapToGrid).initEnvir.initDependantFunc.init(true);
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
      ^max(playStartTime, this.now - (playClock.tempo * Server.default.latency));
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

  now_ { |val, force = true|
    val = max(val, 0);
    if (isPlaying and: force) {
      this.play(val);
    } {
      playbar = val;
      this.changed(\playbar);
    };
  }

  goto { |clipName, point = \start|
    if (clipName.isNumber) {
      this.now = clipName;
    } {
      var clip = this.at(clipName);
      if (clip.notNil) {
        switch (point)
        {\start} { this.now_(clip.startTime); }
        {\end} { this.now_(clip.endTime); }
      };
    };
  }

  stop { |hard = false|
    if (useEnvir) {
      envir.use { tracks.do(_.stop(hard)); };
    } {
      tracks.do(_.stop(hard));
    };

    //if (clock.notNil) { clock.stop; clock = nil };

    //playbar = this.now;
    if (hard) {
      this.prStop;
    } {
      {
        (Server.default.latency * playClock.tempo).wait;
        this.prStop;
      }.fork(playClock);
    };
  }

  prStop {
    isPlaying = false;
    this.changed(\isPlaying, false);
    this.changed(\playbar);
  }

  prMakeClock { |altClock|
    // stop if playing
    if (isPlaying) { this.stop };

    /* will this cause tempoclock buildup?
       // but otherwise this is attempted and failed workaround to endless notes from patterns being cut off by stopping clock
    if (clock.notNil) {
      {
        while { clock.queue != [0] } {
          1.wait;
        };
        clock.stop;
      }.fork(clock);
      clock = nil;
    };
    */

    if (parentClip.notNil and: { parentClip.useParentClock }) {
      playClock = parentClip.track.timeline.playClock;
    } {
      playClock = altClock ?? { clock = TempoClock(tempo); };
    };

    ^playClock;
  }

  play { |startTime, altClock, makeClock = true|
    if (isPlaying) {
      this.stop(true);
    };

    if (makeClock) {
      this.prMakeClock(altClock);
    };

    /*
    if (startTime.notNil) {
      playbar = startTime;
    };
    */

    // save the starting conditions
    //playClock = clock;
    playBeats = playClock.beats;
    playStartTime = startTime ?? playbar;

    if (useEnvir) {
      envir.use {
        tracks.do(_.play(playStartTime, playClock));
      };
    } {
      tracks.do(_.play(playStartTime, playClock));
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

  removeTrack { |index, doFree = true|
    var track = tracks.removeAt(index);
    track.removeDependant(dependantFunc);
    if (doFree) {
      track.free;
    };
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
    //{
      this.prFree;
      //Server.default.sync;
      #tracks, thisTempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView = Object.fromESArray(currentState);
      this.tempo = thisTempo;
      if (clearUndoStack) {
        undoStack = [];
        redoStack = [];
      };
      this.init;
      this.changed(\restoreUndoPoint);
    //}.fork(AppClock)
  }

  new { |clearUndoStack = false|
    this.restoreUndoPoint(this.defaultUndoPoint, clearUndoStack);
    this.changed(\new);
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

  currentClips {
    ^tracks.collect(_.currentClips).flat;
  }

  at { |name|
    var contenders = [];
    this.clips.do { |clip|
      if (clip.name == name) {
        contenders = contenders.add(clip);
      };
    };
    if (contenders.size == 0) {
      if (parentClip.isNil) {
        ^nil
      } {
        ^parentClip.track.timeline.at(name);
      };
    };
    contenders.sort({ |a, b| (a.startTime + (a.duration / 2) - this.now).abs < (b.startTime + (b.duration / 2) - this.now).abs });
    contenders.do { |contender|
      if ((contender.startTime <= this.now) and: (contender.endTime >= this.now)) { ^contender };
    };
    ^contenders[0];
  }

  insertTime { |timeA, timeB|
    tracks.do(_.insertTime(timeA, timeB));
  }

  deleteTime { |timeA, timeB|
    tracks.do(_.deleteTime(timeA, timeB));
  }
}

