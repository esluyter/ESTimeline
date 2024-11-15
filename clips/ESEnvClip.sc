ESEnvClip : ESClip {
  var <env, <bus, <>target, <>addAction, <min, <max, <>curve, <>isExponential, <makeBus = false, <makeBusRate, <useLiveInput, <>liveInput, <>ccNum, <armed, <>midiChannel, <>midiSmooth;
  var <synth, envPlayRout;
  var <recordedLevels, <recordedTimes, <oscFunc, <recordedOffset, <midiFunc, <midiFuncOff, <midiRout;
  var prevLevel = 0.5;

  //classvar <buses;  // event format name -> [bus, nClips] -- when nClips becomes 0 bus should be freed.

  min_ { |val| min = val; this.changed(\min); }
  max_ { |val| max = val; this.changed(\max); }
  env_ { |val| env = val; this.sanitizeEnv; this.changed(\env); }
  bus_ { |val| bus = val; this.changed(\bus); }
  armed_ { |val| armed = val; if (armed) { useLiveInput = true }; this.changed(\armed); }
  useLiveInput_ { |val| useLiveInput = val; if (useLiveInput.not) { armed = false }; this.changed(\useLiveInput) }
  rate { ^this.bus.value.rate }
  makeBus_ { |val|
    if (val != makeBus) {
      this.cleanup;
      makeBus = val;
      this.prep;
      this.changed(\makeBus);
    };
  }
  makeBusRate_ { |val|
    if (val != makeBusRate) {
      if (makeBus) {
        this.cleanup;
        makeBusRate = val;
        this.prep;
      } {
        makeBusRate = val;
      };
      this.changed(\makeBusRate);
    };
  }


  name_ { |val|
    if (val != name) {
      this.cleanup;

      if (val.asString.size > 0) {
        name = val;
      } {
        name = nil;
      };

      this.prep;
      this.changed(\name, val);
    };

  }

  *initClass {
    var sizes = [2, 4, 8, 16, 32, 64, 128, 256, 512]; //, 1024, 2048     //, 4096, 8190]; // 8191 crashes server;
    // cap at 512 so booting server doesn't take forever
    ServerBoot.add {
      {
        sizes.do { |n|
          [\kr, \ar].do { |rate|
            [\curve, \exp].do { |type|
              SynthDef(('ESEnvClip_' ++ rate ++ '_' ++ type ++ '_' ++ n).asSymbol, { |out, gate = 1, tempo = 1, min = 0, max = 1|
                var env = \env.kr(Env.newClear(n).asArray);
                var index = Sweep.perform(rate, 0, tempo);
                var sig = IEnvGen.perform(rate, env, index);

                switch (type)
                {\curve} {
                  sig = sig.lincurve(0, 1, min, max, \curve.kr(0));
                }
                {\exp} {
                  sig = sig.linexp(0, 1, min, max);
                };
                FreeSelf.kr(gate <= 0);

                ReplaceOut.perform(rate, out, sig);
              }).add;
              0.1.wait;
            };
          };
        };
        [\kr, \ar].do { |rate|
          [\curve, \exp].do { |type|
            SynthDef(('ESEnvClip_' ++ rate ++ '_' ++ type ++ '_mouseX').asSymbol, { |out, gate = 1, tempo = 1, min = 0, max = 1, id|
              var index = Sweep.perform(rate, 0, tempo);
              var sig = MouseX.kr;
              SendReply.kr(Impulse.kr(20), "/mouse", [index, sig], id);
              if (rate == \ar) {
                sig = K2A.ar(sig);
              };

              switch (type)
              {\curve} {
                sig = sig.lincurve(0, 1, min, max, \curve.kr(0));
              }
              {\exp} {
                sig = sig.linexp(0, 1, min, max);
              };
              FreeSelf.kr(gate <= 0);

              Out.perform(rate, out, sig);
            }).add;
            SynthDef(('ESEnvClip_' ++ rate ++ '_' ++ type ++ '_mouseY').asSymbol, { |out, gate = 1, tempo = 1, min = 0, max = 1, id|
              var index = Sweep.perform(rate, 0, tempo);
              var sig = MouseY.kr;
              SendReply.kr(Impulse.kr(20), "/mouse", [index, sig], id);
              if (rate == \ar) {
                sig = K2A.ar(sig);
              };

              switch (type)
              {\curve} {
                sig = sig.lincurve(0, 1, min, max, \curve.kr(0));
              }
              {\exp} {
                sig = sig.linexp(0, 1, min, max);
              };
              FreeSelf.kr(gate <= 0);

              Out.perform(rate, out, sig);
            }).add;
            SynthDef(('ESEnvClip_' ++ rate ++ '_' ++ type ++ '_midi').asSymbol, { |out, gate = 1, tempo = 1, min = 0, max = 1, val, lag|
              var sig = val;

              if (rate == \ar) {
                sig = K2A.ar(sig);
              };

              sig = Lag2.perform(rate, sig, lag);

              switch (type)
              {\curve} {
                sig = sig.lincurve(0, 1, min, max, \curve.kr(0));
              }
              {\exp} {
                sig = sig.linexp(0, 1, min, max);
              };
              FreeSelf.kr(gate <= 0);

              Out.perform(rate, out, sig);
            }).add;
            0.1.wait;
          };
        };
      }.fork(SystemClock);
    };
  }

  *new { |startTime, duration, offset = 0, color, name, env, bus, target, addAction = 'addToHead', min = 0, max = 1, curve = 0, isExponential = false, makeBus = true, makeBusRate = \control, mute = false, useLiveInput = false, liveInput = 0, ccNum = 0, armed = false, midiChannel = 16, midiSmooth = 0.1, prep = false|
    env = env ?? Env([0.5, 0.5], [0], [0]);
    ^super.new(startTime, duration, offset, color, name, mute: mute).init(env, bus, target, addAction, min, max, curve, isExponential, makeBus, makeBusRate, mute, useLiveInput, liveInput, ccNum, armed, midiChannel, midiSmooth, prep);
  }

  storeArgs { ^[startTime, duration, offset, color, name, env, bus, target, addAction, min, max, curve, isExponential, makeBus, makeBusRate, mute, useLiveInput, liveInput, ccNum, armed, midiChannel, midiSmooth] }
/*
  duplicate {
    //this.asCompileString.interpret.track_(track);
    ^this.class.new(*(this.storeArgs)).track_(track).prep;
  }
*/
  init { |argEnv, argBus, argTarget, argAddAction, argMin, argMax, argCurve, argExp, argMakeBus, argMakeBusRate, argMute, argUseLiveInput, argLiveInput, argCcNum, argArmed, argMidiChannel, argMidiSmooth, prep|
    env = argEnv;
    bus = argBus;
    target = argTarget;
    addAction = argAddAction;
    min = argMin;
    max = argMax;
    curve = argCurve;
    isExponential = argExp;
    makeBusRate = argMakeBusRate;
    makeBus = argMakeBus;
    useLiveInput = argUseLiveInput;
    liveInput = argLiveInput;
    ccNum = argCcNum;
    armed = argArmed;
    midiChannel = argMidiChannel;
    midiSmooth = argMidiSmooth;

    this.sanitizeEnv;
    if (prep) { this.prep };
  }

  prep {
    if (makeBus) {
      //"allocating bus".postln;
      if (name.notNil) {
        var buses = track.timeline.buses;
        // for named clip, allocate a global bus or increase its clip count
        if (buses[name].notNil) {
          buses[name][1] = buses[name][1] + 1;
        } {
          buses[name] = [Bus.perform(makeBusRate, Server.default, 1), 1];
        };
        bus = buses[name][0];
      } {
        // for unnamed clip, always make new bus
        bus = Bus.perform(makeBusRate, Server.default, 1);
      }
    };
  }

  cleanup {
    if (makeBus) {
      if (bus.notNil) { // if bus is nil, we've already cleaned up
        //"freeing bus".postln;
        if (name.notNil) {
          var buses = track.timeline.buses;
          // for named clip, free or decrement the global bus
          if (buses[name].notNil) {
            buses[name][1] = buses[name][1] - 1;
            if (buses[name][1] < 1) {
              bus.free;
              buses[name] = nil;
            };
          };
        } {
          // for unnamed clip, always free the bus you made
          if (bus.index.notNil) { bus.free; };
        };
      };
    };
    bus = nil;
  }

  prStop {
    if (envPlayRout.notNil) { envPlayRout.stop; envPlayRout = nil; };
    Server.default.bind { synth.release };
    {
      Server.default.latency.wait;
      synth = nil;
      midiFunc.free; midiFunc = nil;
      midiFuncOff.free; midiFuncOff = nil;
      midiRout.stop; midiRout = nil;
    }.fork(SystemClock);

    if (useLiveInput) {
      {
        (Server.default.latency * 2).wait; // make sure all OSC messages have been recieved
        oscFunc.free; oscFunc = nil;

        if (armed) {
          if (recordedLevels.size == 1) {
            recordedLevels = recordedLevels.add(recordedLevels[0]);
            recordedTimes = [if (duration == inf) { 0.1 } { duration }];
          };
          this.env = Env(recordedLevels, recordedTimes, 0);
          this.armed = false;
          this.useLiveInput = false;
          this.offset = recordedOffset;
        };
      }.fork(SystemClock);
    };

  }

  prEnvs { |thisEnv|
    // break long envs apart into 512-sized chunks
    var envs = [];
    var levels = thisEnv.levels;
    var times = thisEnv.times;
    var curves = thisEnv.curves;
    if (curves.isArray.not) { curves = curves.dup(times.size) };
    while { levels.size > 512 } {
      var thisLevels = levels[0..511];
      var thisTimes = times[0..510];
      var thisCurves = curves[0..510];
      levels = levels[511..];
      times = times[511..];
      curves = curves[511..];
      envs = envs.add(Env(thisLevels, thisTimes, thisCurves));
    };
    envs = envs.add(Env(levels, times, curves));
    ^envs;
  }

  prStart { |startOffset = 0.0, clock|
    if (bus.value.isNil) { ^false };

    if (useLiveInput.not) {
      var defName = if (this.rate == 'control') { 'ESEnvClip_kr' } { 'ESEnvClip_ar' } ++ if (this.isExponential) { "_exp_" } { "_curve_" };
      var envs = this.prEnvs(this.envToPlay(startOffset, true)); // break long envs into 512-sized chunks

      envPlayRout = {
        envs.do { |e|
          var size = e.levels.size.nextPowerOfTwo;
          var time = e.duration;
          if (size > 512) {
            ("ERROR: Envelope should have max 512 points. This envelope needs " ++ size) .error;
          };
          Server.default.bind {
            synth.release;
            synth = Synth((defName ++ size).asSymbol, [env: e.asArrayForInterpolation.collect(_.reference).unbubble, out: bus.value, tempo: clock.tempo, min: min, max: max, curve: curve], target.value, addAction.value);
          };
          time.wait;
        };
      }.fork(clock);
    } { // useLiveInput
      if (bus.value.notNil) {
        var defName = if (this.rate == 'control') { 'ESEnvClip_kr' } { 'ESEnvClip_ar' };
        defName = defName ++ if (this.isExponential) { "_exp_" } { "_curve_" };

        if (liveInput < 2) { // mouse input
          defName = (defName ++ ["mouseX", "mouseY"][liveInput]).asSymbol;
          Server.default.bind {
            synth = Synth(defName, [out: bus.value, tempo: clock.tempo, min: min, max: max, curve: curve, id: id], target.value, addAction.value);
          };

          if (armed) {
            //var prevLevel;
            var prevTime;
            var prevPointTime;
            recordedLevels = [];
            recordedTimes = [];
            recordedOffset = startOffset * -1;

            oscFunc = OSCFunc({ |msg|
              var thisId, time, level;
              #thisId, time, level = msg[2..];
              if (id == thisId) {
                if (level != prevLevel) {
                  if (recordedLevels.size == 0) {
                    recordedLevels = [level];
                  } {
                    if (prevTime > prevPointTime) {
                      recordedLevels = recordedLevels.add(prevLevel);
                      recordedTimes = recordedTimes.add(prevTime - prevPointTime);
                    };
                    recordedLevels = recordedLevels.add(level);
                    recordedTimes = recordedTimes.add(time - prevTime);
                  };
                  prevPointTime = time;
                };
                prevTime = time;
                prevLevel = level;
              };
            }, "/mouse");
          } { // not armed
            oscFunc = OSCFunc({ |msg|
              var thisId, time, level;
              #thisId, time, level = msg[2..];
              if (id == thisId) {
                prevLevel = level;
              };
            }, "/mouse");
          };
        } { // midi input
          var chan = if (midiChannel == 16) { nil } { midiChannel };
          prevLevel = switch (liveInput)
          { 2 } { track.timeline.listener.ccValue(ccNum, midiChannel) }
          { 3 } { track.timeline.listener.bendValue(midiChannel) }
          { 4 } { track.timeline.listener.noteValue(midiChannel) }
          { 5 } { track.timeline.listener.noteValue(midiChannel) }
          { 6 } { track.timeline.listener.velValue(midiChannel) }
          { 0 };

          defName = (defName ++ "midi").asSymbol;
          Server.default.bind {
            synth = Synth(defName, [out: bus.value, min: min, max: max, curve: curve, val: prevLevel, lag: midiSmooth], target.value, addAction.value);
          };

          {
            Server.default.latency.wait;
            switch (liveInput)
            { 2 } { // cc
              midiFunc = MIDIFunc.cc({ |midiVal|
                prevLevel = midiVal.linlin(0, 127, 0.0, 1.0);
                synth.set(\val, prevLevel)
              }, ccNum, chan);
            }
            { 3 } { // pitch bend
              midiFunc = MIDIFunc.bend({ |midiVal|
                prevLevel = midiVal.linlin(0, 16383, 0.0, 1.0);
                synth.set(\val, prevLevel);
              }, chan);
            }
            { 4 } { // note
              midiFunc = MIDIFunc.noteOn({ |vel, num|
                prevLevel = num.linlin(0, 127, 0.0, 1.0);
                synth.set(\val, prevLevel);
              }, chan: chan);
            }
            { 5 } { // mono note
              var notes = [];
              midiFunc = MIDIFunc.noteOn({ |vel, num|
                if (notes.indexOf(num).notNil) { notes.remove(num) };
                notes = notes.add(num);
                prevLevel = num.linlin(0, 127, 0.0, 1.0);
                synth.set(\val, prevLevel);
              }, chan: chan);
              midiFuncOff = MIDIFunc.noteOff({ |vel, num|
                notes.remove(num);
                if (notes.size > 0) {
                  prevLevel = notes.last.linlin(0, 127, 0.0, 1.0);
                  synth.set(\val, prevLevel);
                };
              }, chan: chan);
            }
            { 6 } { // vel
              midiFunc = MIDIFunc.noteOn({ |vel, num|
                prevLevel = vel.linlin(0, 127, 0.0, 1.0);
                synth.set(\val, prevLevel);
              }, chan: chan);
            }
            { 7 } { // gated vel
              var notes = [];
              midiFunc = MIDIFunc.noteOn({ |vel, num|
                if (notes.indexOf(num).notNil) { notes.remove(num) };
                notes = notes.add(num);
                prevLevel = vel.linlin(0, 127, 0.0, 1.0);
                synth.set(\val, prevLevel);
              }, chan: chan);
              midiFuncOff = MIDIFunc.noteOff({ |vel, num|
                notes.remove(num);
                if (notes.size == 0) {
                  prevLevel = 0;
                  synth.set(\val, prevLevel);
                };
              }, chan: chan);
            }
            { "This MIDI input not yet implemented".warn; };

            if (armed) {
              var prevPrevLevel = prevLevel;
              var prevTime = track.timeline.soundingNow;
              var prevPointTime = prevTime;
              recordedLevels = [prevLevel];
              recordedTimes = [];
              recordedOffset = startOffset * -1;

              midiRout = {
                var time, level;

                loop {
                  level = prevLevel;
                  time = track.timeline.soundingNow;
                  if (level != prevPrevLevel) {
                    var smoothThresh = midiSmooth / 2;//if ((liveInput == 2) or: (liveInput == 3)) { 0.1 } { 0 };
                    if ((prevTime - prevPointTime) > smoothThresh) {
                      prevTime = prevTime - smoothThresh;
                      recordedLevels = recordedLevels.add(prevPrevLevel);
                      recordedTimes = recordedTimes.add(prevTime - prevPointTime);
                    } {
                      prevTime = prevPointTime;
                    };
                    recordedLevels = recordedLevels.add(level);
                    recordedTimes = recordedTimes.add(time - prevTime);
                    prevPointTime = time;
                  };
                  prevTime = time;
                  prevPrevLevel = level;
                  0.005.wait;
                };
              }.fork(SystemClock);
            };
          }.fork(SystemClock);
        };
      };
    };
  }

  // this is called by parent timeline whenever its tempo changes
  prTempoChanged { |tempo|
    if (synth.notNil) {
      Server.default.bind {
        synth.set(\tempo, tempo);
      };
    };
  }

  prTitle { ^"Env" }

  defaultColor { ^if (useLiveInput) { Color.hsv(0.56, 0.45, 0.75, 0.7) } { Color.hsv(0.58, 0.45, 0.65, 0.7) } }

  guiClass { ^ESEnvClipEditView }
  drawClass { ^ESDrawEnvClip }

  sanitizeEnv {
    var thisEnv = env.value;
    var levels = thisEnv.levels;
    var times = thisEnv.times;
    var curves = thisEnv.curves;
    var changed = false;

    if (times.size > (levels.size - 1)) {
      times = times[0..(levels.size - 2)];
      changed = true;
    };
    if (times.size < (levels.size - 1)) {
      times = times ++ (0.dup(levels.size - 1 - times.size));
      changed = true;
    };

    if (curves.isArray) {
      if (curves.size > (levels.size - 1)) {
        curves = curves[0..(levels.size - 2)];
        changed = true;
      };
      if (curves.size < (levels.size - 1)) {
        curves = curves ++ (0.dup(levels.size - 1 - times.size));
        changed = true;
      };
      curves = curves.collect { |curve|
        if (curve.isNil) {
          changed = true;
          0;
        } {
          curve;
        }
      }
    };

    if (changed) {
      "sanitized env:".postln;
      env = Env(levels, times, curves).postcs;
    }
  }

  envToPlay { |startOffset = 0, addFinalBreakpoint = false|
    var thisEnv = env.value;
    var playOffset = offset + startOffset;
    var initlevel = thisEnv[playOffset];
    var endLevel;

    var levels = thisEnv.levels;
    var times = thisEnv.times;
    var curves = thisEnv.curves;

    var thisDuration = if (duration == inf) { thisEnv.duration } { duration } - startOffset;

    if (playOffset < 0) {
      levels = [levels[0]] ++ levels;
      times = [playOffset * -1] ++ times;
      curves = if (curves.isArray) { [curves[0]] ++ curves } { curves };
    };
    if (playOffset > 0) {
      var runningtime = 0.0;
      var i = 0;
      while { (runningtime < playOffset) && (i < times.size) } {
        runningtime = runningtime + times[i];
        i = i + 1;
      };
      if (runningtime >= playOffset) { // passed target
        var timetokeep = runningtime - playOffset;
        levels = levels[(i - 1)..levels.size];
        times = times[(i - 1)..times.size];
        curves = if (curves.isArray) { curves[(i - 1)..curves.size] } { curves };
        times[0] = timetokeep;
        levels[0] = initlevel;
      } { // ran out of envelope
        times = [0];
        levels = initlevel ! 2;
        curves = \lin;
      };
    };

    endLevel = Env(levels, times, curves)[thisDuration];

    if (times.sum > thisDuration) {
      var prevTime = 0;
      var runningtime = 0.0;
      var i = 0;
      while { (runningtime < thisDuration) && (i < times.size) } {
        prevTime = runningtime;
        runningtime = runningtime + times[i];
        i = i + 1;
      };
      if (i > 0) {
        i = i - 1;
      };
      levels = levels[0..i+1];
      times = times[0..i];
      curves = if (curves.isArray) { curves[0..i] } { curves };

      times[i] = thisDuration - prevTime;
      levels[i+1] = endLevel;
    };

    if (addFinalBreakpoint) {
      if (times.sum < thisDuration) {
        levels = levels.add(endLevel);
        times = times.add(thisDuration - times.sum);
        if (curves.isArray) { curves = curves.add(curves.last) };
      };
    };

    ^Env(levels, times, curves);
  }

  valueNow {
    if (useLiveInput) {
      ^this.prValueScale(prevLevel);
    } {
      ^this.valueAtTime(track.timeline.now - startTime);
    }
  }

  valueAtIndex { |i|
    ^this.prValueScale(this.envToPlay.levels[i]);
  }

  valueAtTime { |time|
    ^this.prValueScale(this.envToPlay.at(time));
  }

  prValueScale { |level|
    if (isExponential) {
      ^level.linexp(0.0, 1.0, min, max);
    } {
      ^level.lincurve(0.0, 1.0, min, max, curve);
    };
  }

  prValueUnscale { |value|
    if (isExponential) {
      ^value.explin(min, max, 0.0, 1.0);
    } {
      ^value.curvelin(min, max, 0.0, 1.0, curve);
    };
  }

  asMap { ^bus.asMap }

  prHasOffset { ^true } // whether to show offset parameter for editing

  /*
  embedInStream { |inval|
    ^this.asMap.embedInStream(inval);
  }
  */
  asStream {
    ^Routine({ arg inval;
      loop {
        this.track.timeline[this.name].embedInStream(inval)
      }
    })
  }
  embedInStream { arg inval;
    //used to be this.asMap
    { this.valueNow }.value(inval).embedInStream(inval);
    ^inval;
	}

  asPattern {
    ^Pfunc { this.track.timeline[this.name].valueNow }
  }

  /*
  asStream { /*^Routine { loop { this.asMap.yield } }*/
    ^Pn(Plazy { this.asMap }, inf).asStream; // so it will update with new envelopes/buses as they become current
  }
  */

  + { ^this }
}