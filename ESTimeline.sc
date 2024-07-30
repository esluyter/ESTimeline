ESTimeline {
  var <tracks, tempo, <>prepFunc, <>cleanupFunc, <>bootOnPrep, <>useEnvir, <>optimizeView, <gridDivision, <snapToGrid, <useMixerChannel, <mixerChannelTemplates, <globalMixerChannelNames, <envHeightMultiplier = 0.5;
  var <isPlaying = false;
  var <playbar = 0.0;
  var playBeats, playStartTime, <playClock;
  var dependantFunc, templateDependantFunc;
  var <>undoStack, <>redoStack, <>currentState;
  var <envir;
  var <>parentClip;
  var clock; // this specifically refers to the internal clock to this specific timeline
  var <buses;
  var <mixerChannels;

  var <>notifyOnEndInitMixerChannels = false;
  classvar <freeQueue;

  classvar <nextId = 0, <timelines;
  var <id; // for referencing e.g. with faders on global mixer

  *initClass { freeQueue = []; timelines = (); }

  *at { |val| ^timelines[val] }

  tempo {
    if (parentClip.notNil and: { parentClip.useParentClock }) {
      ^parentClip.track.timeline.tempo;
    } {
      ^tempo;
    };
  }
  tempo_ { |val|
    if (parentClip.notNil and: { parentClip.useParentClock }) {
      parentClip.track.timeline.tempo = val;
    } {
      tempo = val;
      if (clock.notNil) {
        clock.tempo_(val);
        this.currentClips.do(_.prTempoChanged(val));
      };
    };
    this.changed(\tempo, val);
  }
  tempoBPM { ^this.tempo * 60 }
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

  /*
  defaultMixerChannelTemplate {
    ^(inChannels: 2, outChannels: 2, level: 1, pan: 0, fx: [], preSends: [], postSends: [], envs: (level: nil, pan: nil, fx: [], preSends: [], postSends: []));
  }
  */

  storeArgs { ^[tracks, this.tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, gridDivision, snapToGrid, useMixerChannel, mixerChannelTemplates, globalMixerChannelNames] }
  defaultUndoPoint { ^[[ESTrack([])], 1, nil, nil, bootOnPrep, useEnvir, optimizeView, 4, false, useMixerChannel, (), [\master]].asESArray }

  *newInitMixerChannels { |tracks, tempo = 1, prepFunc, cleanupFunc, bootOnPrep = true, useEnvir = true, optimizeView = true, gridDivision = 4, snapToGrid = false, useMixerChannel = true, mixerChannelTemplates, globalMixerChannelNames|
    //var clock = TempoClock(tempo).permanent_(true);

    tracks = tracks ?? [ESTrack()];
    mixerChannelTemplates = mixerChannelTemplates ?? ();
    globalMixerChannelNames = globalMixerChannelNames ?? [\master];

    ^super.newCopyArgs(tracks, tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, gridDivision, snapToGrid, useMixerChannel, mixerChannelTemplates, globalMixerChannelNames).initId.initEnvir.initDependantFunc.init(true);
  }

  *new { |tracks, tempo = 1, prepFunc, cleanupFunc, bootOnPrep = true, useEnvir = true, optimizeView = false, gridDivision = 4, snapToGrid = false, useMixerChannel = true, mixerChannelTemplates, globalMixerChannelNames|
    tracks = tracks ?? [ESTrack()];
    mixerChannelTemplates = mixerChannelTemplates ?? ();
    globalMixerChannelNames = globalMixerChannelNames ?? [\master];

    ^super.newCopyArgs(tracks, tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, gridDivision, snapToGrid, useMixerChannel, mixerChannelTemplates, globalMixerChannelNames).initId.initEnvir.initDependantFunc.init(true, initMixerChannels: false);
  }

  initId {
    id = nextId;
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

  // for emergencies...
  emptyFreeQueue {
    freeQueue = [];
  }

  prFreeMixerChannels { |callback|
    if (useMixerChannel) {
      var func = {
        mixerChannels.do { |mc|
          mc.releaseDependants;
          mc.free;
        };
        callback.value;
        //freeQueue.remove(func);
      };

      MixerChannelReconstructor.queueDelay = 0.0001;
      this.prAddFuncToEndOfFreeQueue(func);
    }
  }

  prAddFuncToEndOfFreeQueue { |callback|
    var func = {
      callback.value;
      freeQueue.remove(func);
    };
    freeQueue = freeQueue.add(func);
    {
      while { freeQueue[0] != func } { 0.01.wait };

      MixerChannelReconstructor.queueBundle(Server.default, nil, (func: func));
    }.fork(SystemClock);
  }

  subTimelines { |level = 0|
    var ret = [level->this];
    notifyOnEndInitMixerChannels = false;
    this.clips.do { |clip|
      if (clip.class == ESTimelineClip) {
        ret = ret.add(clip.timeline.subTimelines(level + 1));
      };
    };
    ^ret;
  }

  initMixerChannels { |first = true|
    this.changed(\beginInitMixerChannels);

    if (first) {
      var maxLevel = 0;
      var thisTimeline;
      this.subTimelines.flat.do { |assoc|
        if (assoc.key > maxLevel) { maxLevel = assoc.key };
      };
      this.subTimelines.flat.do { |assoc|
        if (assoc.key == maxLevel) { thisTimeline = assoc.value };
      };
      thisTimeline.notifyOnEndInitMixerChannels = true;
    };

    // already checks if we're usingMixerChannel
    this.prFreeMixerChannels({
      var defaultOutbus;
      var newTemplates = ();

      mixerChannels = ();

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
        //var template = this.defaultMixerChannelTemplate ++ (mixerChannelTemplates[name] ?? ());
        var template = mixerChannelTemplates[name] ?? ESMixerChannelTemplate();
        newTemplates[name] = template;
        if (mixerChannels[name].isNil) {
          var outbus = if (name == \master) { defaultOutbus } { mixerChannels[\master] ?? defaultOutbus };
          mixerChannels[name] = MixerChannel(name.asSymbol, Server.default, template.inChannels, template.outChannels, template.level, template.pan, outbus: outbus);
        };
      };

      tracks.do { |track|
        var name = track.mixerChannelName;
        //var template = this.defaultMixerChannelTemplate ++ (mixerChannelTemplates[name] ?? ());
        var template = mixerChannelTemplates[name] ?? ESMixerChannelTemplate();
        newTemplates[name] = template;
        if (track.useMixerChannel and: mixerChannels[name].isNil) {
          mixerChannels[name] = MixerChannel(name.asSymbol, Server.default, template.inChannels, template.outChannels, template.level, template.pan, outbus: mixerChannels[\master] ?? defaultOutbus);
        };
      };

      // sends
      newTemplates.keysValuesDo { |name, template|
        template.preSends.do { |arr| var sendName = arr[0]; var level = arr[1];
          var mc = this.mixerChannel(sendName);
          if (mc.notNil and: mixerChannels[name].notNil) {
            mixerChannels[name].newPreSend(mc, level);
          };
        };
        template.postSends.do { |arr| var sendName = arr[0]; var level = arr[1];
          var mc = this.mixerChannel(sendName);
          if (mc.notNil and: mixerChannels[name].notNil) {
            mixerChannels[name].newPostSend(mc, level);
          };
        };
      };

      mixerChannelTemplates.do({ |template| template.releaseDependants });
      mixerChannelTemplates = newTemplates;
      mixerChannelTemplates.do(_.addDependant(templateDependantFunc));

      this.clips.do { |clip|
        if (clip.class == ESTimelineClip) {
          clip.initMixerChannels(false);
        };
      };
      if (notifyOnEndInitMixerChannels) {
        this.prAddFuncToEndOfFreeQueue({ this.changed(\endInitMixerChannels) });
      };

      this.changed(\initMixerChannels);
    });

    // this will be replaced by callback function above:
    //mixerChannels = ();

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

  mixerChannel { |name|
    if (mixerChannels[name].notNil) {
      ^mixerChannels[name];
    };
    if (parentClip.notNil) {
      ^parentClip.track.timeline.mixerChannel(name);
    };
    ^nil;
  }

  initDependantFunc {
    dependantFunc = { |theTrack, what, value|
      this.changed(\track, [tracks.indexOf(theTrack), theTrack, what, value].flat);
    };
    templateDependantFunc = { |theTemplate, what, value|
      this.changed(\template, [what, value]);
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

  now_ { |val, force = true, propagate = true|
    val = max(val, 0);
    if (isPlaying and: force) {
      this.play(val);
    } {
      playbar = val;
      if (propagate) {
        // this is ugly and duplicates code in ESTimelineClip
        this.clips.select({ |clip| clip.class == ESTimelineClip }).do(_.refreshChildNow);
        if (parentClip.notNil) { parentClip.refreshParentNow };
      };
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
      envir.use {
        tracks.do(_.stop(hard)); };
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
    // stop effects
    mixerChannelTemplates.keysValuesDo { |name, template|
      var mc = mixerChannels[name];
      // remember: mc might be nil if track's useMixerChannel is off
      if (mc.notNil) {
        mc.effectgroup.release;

        if (template.envs.pan.notNil) {
          mc.stopAuto(\pan);
        };
        if (template.envs.level.notNil) {
          mc.stopAuto(\level);
        };
      };
    };
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
      var parentTimeline = parentClip.track.timeline;
      if (parentTimeline.playClock.isNil or: { parentTimeline.playClock.isRunning.not }) {
        parentTimeline.prMakeClock;
      };
      playClock = parentTimeline.playClock;
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
    // save the starting conditions
    //playClock = clock;
    playBeats = playClock.beats;
    playStartTime = startTime ?? playbar;

    // play effects
    mixerChannelTemplates.keysValuesDo { |name, template|
      var mc = mixerChannels[name];
      // remember: mc might be nil if track's useMixerChannel is off
      if (mc.notNil) {
        var thisEnv, thisDefName;
        var getEnvAndDefName = { |mcEnv|
          var thisEnv = mcEnv.envToPlay(playStartTime, this.duration, true);
          var size = thisEnv.levels.size.nextPowerOfTwo;
          var defName = 'ESEnvClip_kr';
          if (size > 512) {
            "WARNING: Envelope can have max 512 points. Please adjust.".postln;
            size = 512;
          };
          defName = (defName ++ if (mcEnv.isExponential) { "_exp_" } { "_curve_" } ++ size).asSymbol;
          [thisEnv, defName];
        };
        //[name, template, mc].postln;
        template.fx.do { |fx|
          mc.playfx(fx);
        };

        if (template.envs.pan.notNil) {
          var mcEnv = template.envs.pan;
          #thisEnv, thisDefName = getEnvAndDefName.(mcEnv);
          Server.default.bind {
            mc.panAuto(thisDefName, [env: thisEnv, tempo: playClock.tempo, min: mcEnv.min, max: mcEnv.max, curve: mcEnv.curve]);
          };
        };
        if (template.envs.level.notNil) {
          var mcEnv = template.envs.level;
          #thisEnv, thisDefName = getEnvAndDefName.(mcEnv);
          Server.default.bind {
            mc.levelAuto(thisDefName, [env: thisEnv, tempo: playClock.tempo, min: mcEnv.min, max: mcEnv.max, curve: mcEnv.curve]);
          };
        };
      };
    };

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

  addTrack { |index, track, mcTemplate|
    index = index ?? tracks.size;
    track = track ?? { ESTrack([]) };
    track.addDependant(dependantFunc);
    track.timeline = this;
    tracks = tracks.insert(index, track);
    // update mixer templates so all track settings stay as they were
    tracks[index + 1..].reverse.do { |thisTrack|
      if (thisTrack.useMixerChannel and: thisTrack.mixerChannelName.isInteger) {
        mixerChannelTemplates[thisTrack.mixerChannelName] = mixerChannelTemplates[thisTrack.mixerChannelName - 1];
        mixerChannelTemplates[thisTrack.mixerChannelName - 1] = nil;
      };
    };
    mixerChannelTemplates[track.mixerChannelName] = mcTemplate;
    this.initMixerChannels;
    this.changed(\tracks);
  }

  removeTrack { |index, doFree = true, doMcInit = true|
    var track = tracks.removeAt(index);
    track.removeDependant(dependantFunc);
    if (doFree) {
      track.free;
    };
    // update mixer templates so all track settings stay as they were
    tracks[index..].do { |thisTrack|
      if (thisTrack.useMixerChannel and: thisTrack.mixerChannelName.isInteger) {
        mixerChannelTemplates[thisTrack.mixerChannelName] = mixerChannelTemplates[thisTrack.mixerChannelName + 1];
        mixerChannelTemplates[thisTrack.mixerChannelName + 1] = nil;
      };
    };
    if (doMcInit) {
      this.initMixerChannels;
    };
    this.changed(\tracks);
  }

  moveTrack { |fromIndex, toIndex|
    var track = tracks[fromIndex];
    var mcTemplate = mixerChannelTemplates[track.mixerChannelName];
    this.removeTrack(fromIndex, false, false);
    this.addTrack(toIndex, track, mcTemplate);
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

  restoreUndoPoint { |undoPoint, clearUndoStack = false, legacy = false, keepMaster = true|
    var thisTempo;
    // leave gridDivision, snapToGrid, and useMixerChannel as they were
    var dummyGD, dummySTG, dummyUMC;
    if (legacy) {
      currentState = undoPoint.interpret.asESArray;
    };
    currentState = undoPoint;

    this.prFree(false); // don't free mixer channels yet, this will happen in this.init -> this.initMixerChannels

    mixerChannelTemplates.do { |template|
      template.releaseDependants;
    };

    #tracks, thisTempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, dummyGD, dummySTG, dummyUMC, mixerChannelTemplates, globalMixerChannelNames = Object.fromESArray(currentState);

    // legacy support
    mixerChannelTemplates.keysValuesDo { |key, value|
      if (value.class == Event) {
        mixerChannelTemplates[key] = ESMixerChannelTemplate(value.inChannels, value.outChannels, value.level, value.pan, value.fx, value.preSends, value.postSends);
      };
    };

    mixerChannelTemplates = mixerChannelTemplates ?? ();
    if (keepMaster) {
      globalMixerChannelNames = globalMixerChannelNames ?? [\master];
    } {
      globalMixerChannelNames.remove(\master);
    };

    this.tempo = thisTempo;
    if (clearUndoStack) {
      undoStack = [];
      redoStack = [];
    };

    this.init;
    this.changed(\restoreUndoPoint);
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

  prFree { |freeMixerChannels = true|
    if (freeMixerChannels) {
      this.prFreeMixerChannels;
    };
    this.cleanup;
    tracks.do(_.free);
  }

  free {
    this.prFree;
    this.changed(\free);
    this.releaseDependants;
    mixerChannelTemplates.do({ |template| template.releaseDependants });
    timelines[id] = nil;
  }

  encapsulateSelf {
    var duration = this.duration;
    if (duration > 0) {
      var newTimeline = ESTimeline(bootOnPrep: bootOnPrep, gridDivision: gridDivision, snapToGrid: snapToGrid, useMixerChannel: useMixerChannel);
      this.prFree;
      tracks = [ESTrack([ESTimelineClip(0, duration, timeline: newTimeline)])];
      prepFunc = {};
      cleanupFunc = {};
      mixerChannelTemplates.do({ |template| template.releaseDependants });
      mixerChannelTemplates = (master: mixerChannelTemplates[\master]);
      this.init;
      newTimeline.restoreUndoPoint(currentState, keepMaster: false);
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

