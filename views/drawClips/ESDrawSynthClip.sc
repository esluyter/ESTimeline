ESDrawSynthClip : ESDrawClip {
  prDraw { |left, top, width, height, editingMode, clipLeft, clipWidth|
    var font = Font.monospace(10);
    var argsValue = clip.prArgsValue;
    var freqIndex = argsValue.indexOf(\freq);
    var ampIndex = argsValue.indexOf(\amp);
    var strTop;
    var displayFreq;

    if (SynthDescLib.at(clip.defName.value).notNil) {
      displayFreq = SynthDescLib.at(clip.defName.value).controlDict[\freq];
    };
    if (displayFreq.notNil) {
      displayFreq = displayFreq.defaultValue;
    };
    if (freqIndex.notNil) {
      displayFreq = argsValue[freqIndex + 1];
    };

    if (displayFreq.isNumber) {
      var freq = displayFreq;
      var y = freq.explin(20, 20000, top + height, top);
      var amp = if (SynthDescLib.at(clip.defName.value).notNil) { SynthDescLib.at(clip.defName.value).controlDict[\amp] } { nil };
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
        var line = "" ++ key ++ ":  " ++ clip.getArg(key).asESDisplayString ++ " -> " ++ val.value;
        line = ESStringShortener.trim(line, width - 5, font);
        strTop = 22 + (i * 6);
        if (strTop < height) {
          Pen.stringAtPoint(line, (left+3.5)@(top + strTop), font, Color.gray(1.0, 0.55));
        };
      };
    };
    ^clip.prTitle;
  }
}