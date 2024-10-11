ESSynthClip : ESClip {
  var <defName, <args, <>target, <>addAction;
  var <func, <doPlayFunc;
  var <synth;

  // changed: args is now required to be array

  autoDefName { ^("ESSynthClip_temp" ++ id).asSymbol }
  playDefName { ^if (doPlayFunc) { this.autoDefName } { defName.value }}

  defName_ { |val| defName = val; this.changed(\defName, val); }
  args_ { |val| args = val; this.changed(\args, val); }
  doPlayFunc_ { |val| doPlayFunc = val; this.prep; this.changed(\doPlayFunc, val); }
  func_ { |val| func = val; this.prep; this.changed(\func, val); }

  storeArgs { ^[startTime, duration, offset, color, name, defName, args, target, addAction, mute, func, doPlayFunc] }

  *new { |startTime, duration, offset = 0, color, name, defName = 'default', args, target, addAction = 'addToHead', mute = false, func, doPlayFunc = false|
    args = args ?? [];
    func = func ? "{ |freq = 440, amp = 0.1, pan|\n  var sig = SinOsc.ar(freq);\n  Pan2.ar(sig, pan, amp);\n}".interpret;
    ^super.new(startTime, duration, offset, color, name, mute: mute).init(defName, args, target, addAction, func, doPlayFunc);
  }

  init { |argDefName, argArgs, argTarget, argAddAction, argFunc, argDoPlayFunc, argBus|
    defName = argDefName;
    args = argArgs;
    target = argTarget;
    addAction = argAddAction;
    func = argFunc;
    doPlayFunc = argDoPlayFunc;
  }

  prep {
    if (doPlayFunc) {
      func.asSynthDef(fadeTime: 0.001, name: this.autoDefName).add;
    };
  }

  prStop {
    Server.default.bind { synth.release };
    synth = nil;
  }

  prStart { |startOffset = 0.0, clock|
    Server.default.bind {
      if (track.timeline.useMixerChannel and: track.useMixerChannel) {
        synth = track.timeline.mixerChannels[track.mixerChannelName].play(this.playDefName, this.prArgsValue(clock));
      } {
        synth = Synth(this.playDefName, this.prArgsValue(clock), target.value, addAction.value);
      };
    };
  }

  prTitle {
    ^if (doPlayFunc) { "Synth" } { defName.value.asString ++ ": Synth" }
  }

  prHasOffset { ^false } // whether to show offset parameter for editing

  defaultColor { ^Color.hsv(0.63, 0.4, 0.53) }

  guiClass { ^ESSynthClipEditView }
  drawClass { ^ESDrawSynthClip }

  argControls {
    ^SynthDescLib.at(this.playDefName).controls;
  }

  prArgsValue { |clock|
    var ret = [];
    args.pairsDo { |key, val|
      val = val.value;
      if (val.class == Symbol) { val = track.timeline[val]; };
      if (val.class == ESEnvClip) { val = if (val.bus.notNil) { val.bus.asMap } { 0 }; };
      //if (val.class == Bus) { val = val.asMap; };
      ret = ret.add(key).add(val);
    };
    if (ret.indexOf(\sustain).isNil and: clock.notNil) {
      ret = ret ++ [sustain: this.duration * clock.tempo.reciprocal];
    };
    ^ret;
  }

  getArg { |key|
    var index = args.indexOf(key);
    if (index.notNil) {
      ^args[index + 1];
    };
    this.argControls.do { |control|
      if (control.name == key) {
        ^control.defaultValue;
      };
    };
    ^0;
  }

  setArg { |key, val|
    var index = args.indexOf(key);
    if (index.notNil) {
      args[index + 1] = val;
      this.changed(\args, args);
    } {
      this.args = args.add(key).add(val);
    };
  }
}