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

  prDraw { |left, top, width, height, editingMode, clipLeft, clipWidth|
    var font = Font.monospace(10);
    var argsValue = this.prArgsValue;
    var freqIndex = argsValue.indexOf(\freq);
    var ampIndex = argsValue.indexOf(\amp);
    var strTop;
    var displayFreq;

    displayFreq = SynthDescLib.at(defName.value).controlDict[\freq];
    if (displayFreq.notNil) {
      displayFreq = displayFreq.defaultValue;
    };
    if (freqIndex.notNil) {
      displayFreq = argsValue[freqIndex + 1];
    };

    if (displayFreq.isNumber) {
      var freq = displayFreq;
      var y = freq.explin(20, 20000, top + height, top);
      var amp = SynthDescLib.at(defName.value).controlDict[\amp];
      if (amp.notNil) { amp = amp.defaultValue };
      if (ampIndex.notNil) {
        amp = argsValue[ampIndex + 1];
      };
      Pen.addRect(Rect(left, y, width, 2));
      Pen.color = Color.gray(1, if (amp.isNumber) { amp.ampdb.linexp(-60.0, 0.0, 0.05, 1.0) } { 0.5 });
      Pen.fill;
    };

    if (left < 0) {
      width = width + left;
      left = 0;
    };
    if (clipLeft.notNil and: { left < clipLeft }) {
      width = width - (clipLeft - left);
      left = clipLeft;
    };

    if ((height > 30) and: (width > 15)) {
      argsValue.pairsDo { |key, val, i|
        var line = "" ++ key ++ ":  " ++ this.getArg(key).asESDisplayString ++ " -> " ++ val.value;
        line = ESStringShortener.trim(line, width - 5, font);
        strTop = 22 + (i * 6);
        if (strTop < height) {
          Pen.stringAtPoint(line, (left+3.5)@(top + strTop), font, Color.gray(1.0, 0.55));
        };
      };
    };
    ^this.prTitle;
  }

  prTitle {
    ^defName.value.asString ++ ": Synth"
  }

  defaultColor { ^Color.hsv(0.85, 0.45, 0.5) }

  guiClass { ^ESSynthClipEditView }

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