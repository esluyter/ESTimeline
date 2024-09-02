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
        this.prChangeEnvTempos;
      };
    };
    this.changed(\tempo, val);
  }
  envs {
    var envs = [];
    mixerChannelTemplates.do { |template|
      if (template.envs.level.notNil) {
        envs = envs.add(template.envs.level);
      };
      if (template.envs.pan.notNil) {
        envs = envs.add(template.envs.pan);
      };
      template.envs.preSends.do { |env|
        if (env.notNil) {
          envs = envs.add(env);
        };
      };
      template.envs.postSends.do { |env|
        if (env.notNil) {
          envs = envs.add(env);
        };
      };
      template.envs.fx.do { |ev|
        if (ev.notNil) {
          ev.do { |env|
            if (env.notNil) {
              envs = envs.add(env);
            };
          };
        };
      };
    };
    ^envs;
  }
  prChangeEnvTempos {
    var envs = this.envs;
    envs.do { |env|
      if (env.synth.notNil) {
        Server.default.bind {
          env.synth.set(\tempo, tempo);
        };
      };
    };
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
  useMixerChannel_ { |val, initMcs = true|
    if (val == false) {
      this.prFreeMixerChannels;
    };
    useMixerChannel = val;
    this.clips.do { |clip|
      if (clip.class == ESTimelineClip) {
        if (val == false) {
          clip.timeline.prFreeMixerChannels;
        };
        clip.timeline.useMixerChannel_(val, false);
      };
    };
    if (initMcs and: (val == true)) {
      this.initMixerChannels;
    };
    this.changed(\useMixerChannel);
  }

  storeArgs { ^[tracks, this.tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, gridDivision, snapToGrid, useMixerChannel, mixerChannelTemplates, globalMixerChannelNames] }
  defaultUndoPoint { ^[[ESTrack([])], 1, nil, nil, bootOnPrep, useEnvir, optimizeView, 4, false, useMixerChannel, (), [\master]].asESArray }

  *newInitMixerChannels { |tracks, tempo = 1, prepFunc, cleanupFunc, bootOnPrep = true, useEnvir = true, optimizeView = true, gridDivision = 4, snapToGrid = false, useMixerChannel = true, mixerChannelTemplates, globalMixerChannelNames|
    //var clock = TempoClock(tempo).permanent_(true);

    tracks = tracks ?? [ESTrack()];

    mixerChannelTemplates = mixerChannelTemplates ?? ();
    mixerChannelTemplates = ESEvent.newFrom(mixerChannelTemplates);

    globalMixerChannelNames = globalMixerChannelNames ?? [\master];

    ^super.newCopyArgs(tracks, tempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, gridDivision, snapToGrid, useMixerChannel, mixerChannelTemplates, globalMixerChannelNames).initId.initEnvir.initDependantFunc.init(true);
  }

  *new { |tracks, tempo = 1, prepFunc, cleanupFunc, bootOnPrep = true, useEnvir = true, optimizeView = false, gridDivision = 4, snapToGrid = false, useMixerChannel = false, mixerChannelTemplates, globalMixerChannelNames|
    tracks = tracks ?? [ESTrack()];

    mixerChannelTemplates = mixerChannelTemplates ?? ();
    mixerChannelTemplates = ESEvent.newFrom(mixerChannelTemplates);

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

  defaultOutbus {
    if (
      parentClip.notNil and:
      { parentClip.track.useMixerChannel } and:
      { parentClip.track.timeline.useMixerChannel } and:
      { parentClip.track.mixerChannel.notNil } and:
      { parentClip.track.mixerChannel.active }
    ) {
      ^parentClip.track.mixerChannel
    } {
      ^0
    };
  }

  initMixerChannels { |first = true|
    if (useMixerChannel.not) { ^false };

    if (first) {
      var maxLevel = 0;
      var thisTimeline;

      this.changed(\beginInitMixerChannels);

      this.subTimelines.flat.do { |assoc|
        if (assoc.key > maxLevel) { maxLevel = assoc.key };
      };
      this.subTimelines.flat.do { |assoc|
        if (assoc.key == maxLevel) { thisTimeline = assoc.value };
      };
      thisTimeline.notifyOnEndInitMixerChannels = true;
    };

    this.prFreeMixerChannels({
      var defaultOutbus;
      var newTemplates = ESEvent.newFrom(());

      mixerChannels = ();

      //Server.default.sync;
      defaultOutbus = this.defaultOutbus;

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

      // sends + fx prep
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
        template.fx.do { |fx|
          fx.prep;
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
    // stop effects/automation
    mixerChannelTemplates.keysValuesDo { |name, template|
      var mc = mixerChannels[name];
      // remember: mc might be nil if track's useMixerChannel is off
      if (mc.notNil) {
        template.stop(mc);
      };
    };
    this.changed(\isPlaying, false);
    if (parentClip.notNil and: { parentClip.track.timeline.isPlaying.not }) {
      parentClip.track.timeline.changed(\isPlaying, false);
    };
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

    // play effects/automation
    mixerChannelTemplates.keysValuesDo { |name, template|
      var mc = mixerChannels[name];
      // remember: mc might be nil if track's useMixerChannel is off
      if (mc.notNil) {
        template.play(playStartTime, playClock, mc, this.duration);
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
    if (parentClip.notNil and: { parentClip.track.timeline.isPlaying.not }) {
      parentClip.track.timeline.changed(\isPlaying, true);
    };
  }

  togglePlay {
    if (this.isPlaying) { this.stop(true) } { this.play }
  }

  addTrack { |index, track, mcTemplate, mc|
    var initMc = false;
    mcTemplate = mcTemplate ?? ESMixerChannelTemplate();
    index = index ?? tracks.size;
    track = track ?? { ESTrack([]) };
    track.clips.do { |clip|
      if (clip.class == ESTimelineClip) {
        initMc = true;
      };
    };
    if (mc.notNil) {
      initMc = false;
    };
    track.addDependant(dependantFunc);
    track.timeline = this;
    tracks = tracks.insert(index, track);
    // update mixer templates so all track settings stay as they were
    tracks[index + 1..].reverse.do { |thisTrack|
      if (thisTrack.useMixerChannel and: thisTrack.mixerChannelName.isInteger) {
        mixerChannelTemplates[thisTrack.mixerChannelName] = mixerChannelTemplates[thisTrack.mixerChannelName - 1];
        mixerChannelTemplates[thisTrack.mixerChannelName - 1] = nil;

        mixerChannels[thisTrack.mixerChannelName] = mixerChannels[thisTrack.mixerChannelName - 1];
        mixerChannels[thisTrack.mixerChannelName - 1] = nil;
      };
    };
    if (track.useMixerChannel) {
      mixerChannelTemplates[track.mixerChannelName] = mcTemplate;
    };

    if (initMc) {
      this.initMixerChannels;
    } {
      if (useMixerChannel and: track.useMixerChannel and: mixerChannels[track.mixerChannelName].isNil) {
        mixerChannels[track.mixerChannelName] = mc ?? {
          MixerChannel(track.mixerChannelName.asSymbol, Server.default, mcTemplate.inChannels, mcTemplate.outChannels, mcTemplate.level, mcTemplate.pan, outbus: mixerChannels[\master] ?? this.defaultOutbus);
        };
      };

      mcTemplate.addDependant(templateDependantFunc);
    };

    this.changed(\tracks);
  }

  removeTrack { |index, doFree = true, doMcInit = true|
    var track = tracks.at(index);
    var mc, mcName = track.mixerChannelName, hasSameMcName = false;

    tracks.removeAt(index);

    track.removeDependant(dependantFunc);
    if (doFree) {
      track.free;
    };

    mc = mixerChannels[mcName];

    tracks.do { |t|
      if (t.mixerChannelName == mcName) {
        hasSameMcName = true;
      };
    };

    if (track.useMixerChannel and: (mcName.isInteger or: hasSameMcName.not) and: doFree) {
      mc.releaseDependants;
      mc.free;
      mixerChannels[mcName] = nil;
    };

    if (hasSameMcName.not) {
      // this should release template
      mixerChannelTemplates[mcName].releaseDependants;
      mixerChannelTemplates[mcName] = nil;
    };

    // update mixer templates so all track settings stay as they were
    tracks[index..].do { |thisTrack|
      if (thisTrack.useMixerChannel and: thisTrack.mixerChannelName.isInteger) {
        mixerChannelTemplates[thisTrack.mixerChannelName] = mixerChannelTemplates[thisTrack.mixerChannelName + 1];
        mixerChannelTemplates[thisTrack.mixerChannelName + 1] = nil;

        mixerChannels[thisTrack.mixerChannelName] = mixerChannels[thisTrack.mixerChannelName + 1];
        mixerChannels[thisTrack.mixerChannelName + 1] = nil;
      };
    };
    //if (doMcInit) {
      //this.initMixerChannels;
    //};

    this.changed(\tracks);
  }

  moveTrack { |fromIndex, toIndex|
    var track = tracks[fromIndex];
    var mcTemplate = mixerChannelTemplates[track.mixerChannelName];
    var mc = mixerChannels[track.mixerChannelName];
    this.removeTrack(fromIndex, false, false);
    this.addTrack(toIndex, track, mcTemplate, mc);
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

  restoreUndoPoint { |undoPoint, clearUndoStack = false, legacy = false, keepMaster = true, useAllFields = false|
    var arr;
    var thisTempo;
    // leave gridDivision, snapToGrid, and useMixerChannel as they were
    var dummyGD, dummySTG, dummyUMC;
    if (legacy) {
      currentState = undoPoint.interpret.asESArray;
    };
    currentState = undoPoint;

    this.prFree(false); // don't free mixer channels yet, this will happen in this.init -> this.initMixerChannels

    arr = Object.fromESArray(currentState);
    #dummyGD, dummySTG, dummyUMC = arr[7..9];

    if (useAllFields) {
      var oldUMC = useMixerChannel;
      gridDivision = dummyGD;
      snapToGrid = dummySTG;
      if (oldUMC and: dummyUMC.not) {
        // this will free old MCs
        this.useMixerChannel = false;
      } {
        useMixerChannel = dummyUMC;
      };
    };

    mixerChannelTemplates.do { |template|
      template.releaseDependants;
    };

    #tracks, thisTempo, prepFunc, cleanupFunc, bootOnPrep, useEnvir, optimizeView, dummyGD, dummySTG, dummyUMC, mixerChannelTemplates, globalMixerChannelNames = arr;

    mixerChannelTemplates = ESEvent.newFrom(mixerChannelTemplates);

    // prep fx and legacy support
    mixerChannelTemplates.keysValuesDo { |key, value|
      if (value.class == Event) {
        value = ESMixerChannelTemplate(value.inChannels, value.outChannels, value.level, value.pan, value.fx, value.preSends, value.postSends);
        mixerChannelTemplates[key] = value;
      };
      value.fx.do { |fxSynth, i|
        if (fxSynth.isFunction) {
          fxSynth = ESFxSynth(func: fxSynth, doPlayFunc: true);
          value.fx[i] = fxSynth;
        };
        fxSynth.prep;
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

    currentState = this.asUndoPoint;
    this.changed(\restoreUndoPoint);
  }

  new { |clearUndoStack = false|
    this.restoreUndoPoint(this.defaultUndoPoint, clearUndoStack, useAllFields: true);
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
    var endOfLastClip = this.clips.collect(_.endTime).maxItem ?? 0;
    var longestEnvelope = this.envs.collect({ |env| env.env.duration }).maxItem ?? 0;
    ^[endOfLastClip, longestEnvelope].maxItem;
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
    this.envs.do(_.insertTime(timeA, timeB));
  }

  deleteTime { |timeA, timeB|
    tracks.do(_.deleteTime(timeA, timeB));
    this.envs.do(_.deleteTime(timeA, timeB));
  }
}

