ESTimeline {
  var <tracks, <tempo, <>prepFunc, <>cleanupFunc, <>bootOnPrep, <>useEnvir, <>optimizeView, <gridDivision, <snapToGrid, <useMixerChannel, <mixerChannelTemplates, <globalMixerChannelNames;
  var <isPlaying = false;
  var <playbar = 0.0;
  var playBeats, playStartTime, <playClock;
  var dependantFunc;
  var <>undoStack, <>redoStack, <>currentState;
  var <envir;
  var <>parentClip;
  var clock; // this specifically refers to the internal clock to this specific timeline
  var <buses;
  var <mixerChannels;

  classvar <nextId = 0, <timelines;
  var <id; // for referencing e.g. with faders on global mixer

  *at { |val| ^timelines[val] }

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
  useMixerChannel_ { |val|
    useMixerChannel = val;
    this.initMixerChannels;
    this.changed(\useMixerChannel);
  }

  storeArgs { ^[tracks, this.tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, gridDivision, snapToGrid, useMixerChannel, mixerChannelTemplates, globalMixerChannelNames] }
  defaultUndoPoint { ^[[ESTrack([])], 1, nil, nil, bootOnPrep, useEnvir, optimizeView, 4, false, useMixerChannel, mixerChannelTemplates, globalMixerChannelNames].asESArray }

  *new { |tracks, tempo = 1, prepFunc, cleanupFunc, bootOnPrep = true, useEnvir = true, optimizeView = true, gridDivision = 4, snapToGrid = false, useMixerChannel = true, mixerChannelTemplates, globalMixerChannelNames|
    //var clock = TempoClock(tempo).permanent_(true);

    tracks = tracks ?? [ESTrack()];
    mixerChannelTemplates = mixerChannelTemplates ?? ();
    globalMixerChannelNames = globalMixerChannelNames ?? [\master];

    ^super.newCopyArgs(tracks, tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, gridDivision, snapToGrid, useMixerChannel, mixerChannelTemplates, globalMixerChannelNames).initId.initEnvir.initDependantFunc.init(true);
  }

  *newNoInitMixerChannels { |tracks, tempo = 1, prepFunc, cleanupFunc, bootOnPrep = true, useEnvir = true, optimizeView = false, gridDivision = 4, snapToGrid = false, useMixerChannel = true, mixerChannelTemplates, globalMixerChannelNames|
    tracks = tracks ?? [ESTrack()];
    mixerChannelTemplates = mixerChannelTemplates ?? ();
    globalMixerChannelNames = globalMixerChannelNames ?? [\master];

    ^super.newCopyArgs(tracks, tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, gridDivision, snapToGrid, useMixerChannel, mixerChannelTemplates, globalMixerChannelNames).initId.initEnvir.initDependantFunc.init(true, initMixerChannels: false);
  }

  initId {
    id = nextId;
    if (timelines.isNil) { timelines = () };
    timelines[id] = this;
    nextId = nextId + 1;
  }

  initEnvir {
    envir = Environment.make {
      ~timeline = this;
    };
    buses = ();
    mixerChannels = ();
  }

  prFreeMixerChannels { |callback|
    if (useMixerChannel) {
      MixerChannelReconstructor.queueDelay = 0.0001;

      MixerChannelReconstructor.queueBundle(Server.default, nil, (func: {
        mixerChannels.do { |mc|
          mc.release;
          mc.free;
        };
        callback.value;
      }));
    }
  }

  initMixerChannels {
    // already checks if we're usingMixerChannel
    this.prFreeMixerChannels({
      var defaultOutbus;
      var newTemplates = ();

      //Server.default.sync;
      defaultOutbus = if (
        parentClip.notNil and:
        { parentClip.track.useMixerChannel } and:
        { parentClip.track.timeline.useMixerChannel } and:
        { parentClip.track.mixerChannel.notNil } and:
        { parentClip.track.mixerChannel.active }
      ) {
        parentClip.track.mixerChannel
      } {
        0
      };

      // put \master at the end of the global mixer channels for best results
      globalMixerChannelNames.reverse.do { |name|
        var template = mixerChannelTemplates[name] ?? (inChannels: 2, outChannels: 2, level: 1, pan: 0);
        newTemplates[name] = template;
        if (mixerChannels[name].isNil) {
          var outbus = if (name == \master) { defaultOutbus } { mixerChannels[\master] ?? defaultOutbus };
          mixerChannels[name] = MixerChannel(name.asSymbol, Server.default, template.inChannels, template.outChannels, template.level, template.pan, outbus: outbus);

        };
      };

      tracks.do { |track|
        var name = track.mixerChannelName;
        var template = mixerChannelTemplates[name] ?? (inChannels: 2, outChannels: 2, level: 1, pan: 0);
        newTemplates[name] = template;
        if (track.useMixerChannel and: mixerChannels[track.mixerChannelName].isNil) {
          mixerChannels[name] = MixerChannel(name.asSymbol, Server.default, template.inChannels, template.outChannels, template.level, template.pan, outbus: mixerChannels[\master] ?? defaultOutbus);
        };
      };

      mixerChannelTemplates = newTemplates;

      this.clips.do { |clip|
        if (clip.class == ESTimelineClip) {
          clip.initMixerChannels;
        };
      };

      this.changed(\initMixerChannels);
    });

    // this will be replaced by callback function above:
    mixerChannels = ();

    //if (useMixerChannel) {

    //};
  }

  orderedMixerChannels {
    var globalRet = [];
    var ret = [];

    if (useMixerChannel.not) { ^[] };

    globalMixerChannelNames.do { |name|
      var mc = mixerChannels[name];
      if (globalRet.includes(mc).not) {
        globalRet = globalRet.add(mc);
      };
    };
    // reverse so that grouped buses appear before the track that contains them
    tracks.reverse.do { |track|
      var timelineClips;

      // add track channel if applicable
      if (track.useMixerChannel) {
        var mc = mixerChannels[track.mixerChannelName];
        if (ret.includes(mc).not and: globalRet.includes(mc).not) {
          ret = ret.add(mc);
        };
      };

      // add channels for sub-timelines
      timelineClips = track.nowClips.select({ |clip| clip.class == ESTimelineClip });
      if (timelineClips.notNil) {
        timelineClips.do { |timelineClip|
          ret = ret.add(timelineClip.timeline.orderedMixerChannels);
        };
      };
    };
    ^ret.reverse ++ globalRet;
  }

  orderedMixerChannelNames {
    var globalRet = []; // return an array with id and names [2, \bass, \kik, \sn, \master] for three tracks plus master in timeline 2
    var ret = []; // if timeline 2 is embedded in timeline 1, [1, \melody, \harmony, [2, \bass, \kik, \sn, \master], \drums, \fx]

    if (useMixerChannel.not) { ^[] };

    globalMixerChannelNames.do { |name|
      if (globalRet.includes(name).not) {
        globalRet = globalRet.add(name);
      };
    };

    // reverse as stated above
    tracks.reverse.do { |track|
      var timelineClips;

      if (track.useMixerChannel) {
        var name = track.mixerChannelName;
        if (ret.includes(name).not and: globalRet.includes(name).not) {
          ret = ret.add(name);
        };
      };

      // add channels for sub-timelines
      timelineClips = track.nowClips.select({ |clip| clip.class == ESTimelineClip });
      if (timelineClips.notNil) {
        timelineClips.do { |timelineClip|
          ret = ret.add(timelineClip.timeline.orderedMixerChannelNames);
        };
      };
    };
    ^[id] ++ ret.reverse ++ globalRet;
  }

  setMixerChannel { |name, what, val|
    switch (what)
    { \level } {
      mixerChannels[name].level = val;
      mixerChannelTemplates[name].level = val;
    }
    { \pan } {
      mixerChannels[name].pan = val;
      mixerChannelTemplates[name].pan = val;
    }
  }

  initDependantFunc {
    dependantFunc = { |theTrack, what, value|
      this.changed(\track, [tracks.indexOf(theTrack), theTrack, what, value].flat);
    };
  }

  init { |resetUndo = false, cleanupFirst = false, initMixerChannels = true|
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
        this.prep(initMixerChannels);
        Server.default.sync;
        this.changed(\init);
      };
    } {
      this.prep(initMixerChannels);
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
      // this is ugly and duplicates code in ESTimelineClip
      this.clips.select({ |clip| clip.class == ESTimelineClip }).do(_.refreshTimelineNow);
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
    this.initMixerChannels;
    this.changed(\tracks);
  }

  removeTrack { |index, doFree = true|
    var track = tracks.removeAt(index);
    track.removeDependant(dependantFunc);
    if (doFree) {
      track.free;
    };
    this.initMixerChannels;
    this.changed(\tracks);
  }

  asUndoPoint { ^this.storeArgs.asESArray }

  addUndoPoint {
    var undoPoint = this.asUndoPoint;
    if (undoPoint != currentState) {
      undoStack = undoStack.add(currentState);
      if (undoStack.size > 100) {
        undoStack = undoStack[1..];
      };
      currentState = undoPoint;
      redoStack = [];
      this.changed(\addUndoPoint)
    };
  }

  restoreUndoPoint { |undoPoint, clearUndoStack = false, legacy = false|
    var thisTempo;
    // leave gridDivision, snapToGrid, and useMixerChannel as they were
    var dummyGD, dummySTG, dummyUMC;
    if (legacy) {
      currentState = undoPoint.interpret.asESArray;
    };
    currentState = undoPoint;
    //{
    this.prFree;
    //Server.default.sync;
    #tracks, thisTempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, dummyGD, dummySTG, dummyUMC, mixerChannelTemplates, globalMixerChannelNames = Object.fromESArray(currentState);

    mixerChannelTemplates = mixerChannelTemplates ?? ();
    globalMixerChannelNames = globalMixerChannelNames ?? [\master];

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

  prep { |initMixerChannels = true|
    if (initMixerChannels) {
      this.initMixerChannels;
    };

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
    this.prFreeMixerChannels;
    this.cleanup;
    tracks.do(_.free);
  }

  free {
    this.prFree;
    this.changed(\free);
    this.release;
    timelines[id] = nil;
  }

  encapsulateSelf {
    var duration = this.duration;
    if (duration > 0) {
      var newTimeline = ESTimeline.newNoInitMixerChannels(bootOnPrep: bootOnPrep, gridDivision: gridDivision, snapToGrid: snapToGrid, useMixerChannel: useMixerChannel);
      this.prFree;
      tracks = [ESTrack([ESTimelineClip(0, duration, timeline: newTimeline)])];
      prepFunc = {};
      cleanupFunc = {};
      this.init;
      newTimeline.restoreUndoPoint(currentState);
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

