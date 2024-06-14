ESSynthClip : ESClip {
  var <>defName, <>args, <>target, <>addAction;
  var <synth;

  storeArgs { ^[startTime, duration, defName, args, target, addAction, color] }

  *new { |startTime, duration, defName, args, target, addAction = 'addToHead', color|
    ^super.new(startTime, duration, color).init(defName, args, target, addAction);
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

  prDraw { |left, top, width, height|
    var font = Font.monospace(10);
    var argsValue = args.value.asArray;
    var freqIndex = argsValue.indexOf(\freq);
    var ampIndex = argsValue.indexOf(\amp);

    if (left < 0) {
      width = width + left;
      left = 0;
    };

    if ((height > 30) and: (width > 15)) {
      argsValue.pairsDo { |key, val, i|
        var line = "" ++ key ++ ":  " ++ val;
        while { max(0, width - 5) < (QtGUI.stringBounds(line, font).width) } {
          if (line.size == 1) {
            line = "";
          } {
            line = line[0..line.size-2];
          };
        };
        Pen.stringAtPoint(line, (left+3.5)@(top+22+(i * 6)), font, Color.gray(1.0, 0.4));
      };
    };
    if (freqIndex.notNil) {
      var freq = argsValue[freqIndex + 1];
      var y = freq.explin(20, 20000, height, top);
      var amp = 0.2;
      if (ampIndex.notNil) {
        amp = argsValue[ampIndex + 1];
      };
      Pen.addRect(Rect(left, y, width, 2));
      Pen.color = Color.gray(1, amp.ampdb.linexp(-60.0, 0.0, 0.05, 1.0));
      Pen.fill;
    };
    ^defName.value.asString ++ ": Synth";
  }

  defaultColor { ^Color.hsv(0.85, 0.5, 0.5) }

  guiClass { ^ESSynthClipEditView }

  defNameString {
    if (defName.isFunction) {
      var cs = defName.asCompileString;
      if (cs.size == 2) { ^"" };
      ^cs[1..cs.size-2];
    };
    ^defName.asCompileString;
  }

  targetString {
    if (target.isFunction) {
      var cs = target.asCompileString;
      if (cs.size == 2) { ^"" };
      ^cs[1..cs.size-2];
    };
    if (target.isNil) {
      ^"";
    };
    ^target.asCompileString;
  }

  addActionString {
    if (addAction.isFunction) {
      var cs = addAction.asCompileString;
      if (cs.size == 2) { ^"" };
      ^cs[1..cs.size-2];
    };
    ^addAction.asCompileString;
  }

  prArgsValue { |clock|
    var ret = args.value.asArray;
    if (ret.indexOf(\sustain).isNil) {
      ret = ret ++ [sustain: this.duration * clock.tempo.reciprocal];
    };
    ^ret;
  }

  argsString {
    var str;
    if (args.isFunction) {
      var cs = args.asCompileString;
      if (cs.size == 2) { ^"" };
      ^cs[1..cs.size-2];
    };
    if (args.asArray.size == 0) {
      ^"[]";
    };
    //^args.asArray.asCompileString;
    str = "[\n";
    args.pairsDo { |key, val|
      str = str ++ "  " ++ key ++ ": " ++ val.asCompileString ++ ",\n";
    };
    str = str ++ "]";
    ^str;
  }
}