ESSynthClip : ESClip {
  var <defName, <args, <>target, <>addAction;
  var <synth;

  // changed: args is now required to be array

  defName_ { |val| defName = val; this.changed(\defName, val); }
  args_ { |val| args = val; this.changed(\args, val); }

  storeArgs { ^[startTime, duration, offset, color, name, defName, args, target, addAction, mute] }

  *new { |startTime, duration, offset = 0, color, name, defName, args, target, addAction = 'addToHead', mute = false|
    args = args ?? [];
    ^super.new(startTime, duration, offset, color, name, mute: mute).init(defName, args, target, addAction);
  }

  init { |argDefName, argArgs, argTarget, argAddAction|
    defName = argDefName;
    args = argArgs;
    target = argTarget;
    addAction = argAddAction;
  }

  prStop {
    Server.default.bind { synth.release };
    synth = nil;
  }

  prStart { |startOffset = 0.0, clock|
    Server.default.bind {
      synth = Synth(defName.value, this.prArgsValue(clock), target.value, addAction.value)
    };
  }

  prTitle {
    ^defName.value.asString ++ ": Synth"
  }

  prHasOffset { ^false } // whether to show offset parameter for editing

  defaultColor { ^Color.hsv(0.85, 0.45, 0.5) }

  guiClass { ^ESSynthClipEditView }
  drawClass { ^ESDrawSynthClip }

  argControls {
    ^SynthDescLib.at(defName.value).controls;
  }

  prArgsValue { |clock|
    var ret = [];
    args.pairsDo { |key, val|
      val = val.value;
      if (val.class == Symbol) { val = track.timeline[val]; };
      if (val.class == ESEnvClip) { val = val.bus.asMap; };
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