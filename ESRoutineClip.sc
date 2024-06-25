ESRoutineClip : ESClip {
  var <routine, <>randSeed, <>isSeeded, <>addLatency,
  <>fastForward, // 0 - don't play from middle, 1 - fast forward from middle, 2 - start from beginning always
  <>cleanupFunc, <func;
  var player;

  storeArgs { ^[startTime, duration, offset, color, name, func, randSeed, isSeeded, addLatency, fastForward, cleanupFunc] }

  *new { |startTime, duration, offset = 0, color, name, func, randSeed, isSeeded = true, addLatency = false, fastForward = 1, cleanupFunc|
    ^super.new(startTime, duration, offset, color, name).init(func, randSeed, isSeeded, addLatency, fastForward, cleanupFunc);
  }

  init { |argFunc, argRandSeed, argIsSeeded, argAddLatency, argFastForward, argCleanupFunc|
    //routine = ESRoutine(argFunc);
    func = argFunc;
    randSeed = argRandSeed ?? rand(2000000000);
    isSeeded = argIsSeeded;
    addLatency = argAddLatency;
    fastForward = argFastForward;
    cleanupFunc = argCleanupFunc;
  }

  prStop {
    if (isPlaying) {
      {
        if (addLatency) {
          // adjust for server latency
          Server.default.latency.wait;
        };
        player.stop;
        routine.stop;
        cleanupFunc.value;
      }.fork(SystemClock);
    }
  }

  prStart { |startOffset = 0.0, clock|
    startOffset = startOffset + offset;
    routine = ESRoutine(func);
    if (isSeeded) {
      // set random seed
      routine.randSeed = randSeed;
    };
    if ((startOffset > 0) and: (fastForward == 0)) {
      // don't play from middle
      isPlaying = false;
    } {
      player = {
        if (fastForward == 1) {
          // fast forward
          routine.fastForward(startOffset).wait;
        };
        if (addLatency) {
          // adjust for server latency
          (Server.default.latency * clock.tempo).wait;
        };
        routine.play(clock);
      }.fork(clock);
    };
  }

  func_ { |val|
    func = val;
    this.changed(\func, val);
  }

  prDraw { |left, top, width, height, editingMode, clipLeft, clipWidth|
    if (left < 0) {
      width = width + left;
      left = 0;
    };
    if (clipLeft.notNil and: { left < clipLeft }) {
      width = width - (clipLeft - left);
      left = clipLeft;
    };

    if ((height > 30) and: (width > 15)) {
      var string = this.func.asESDisplayString;
      var lines = string.split($\n);
      var font = Font.monospace(10);
      var funcHeight = lines.size * 10;
      var strTop;
      lines.do { |line, i|
        while { max(0, width - 5) < (QtGUI.stringBounds(line, font).width) } {
          if (line.size == 1) {
            line = "";
          } {
            line = line[0..line.size-2];
          };
        };
        strTop = 20 + (i * 10);
        if (strTop < height) {
          Pen.stringAtPoint(line, (left+3.5)@(top+strTop), font, Color.gray(1.0, 0.6));
        };
      };
      if (25 + funcHeight < height) {
        Pen.addRect(Rect(left, top + 25 + funcHeight, width / 2, 1));
        Pen.color = Color.gray(1.0, 0.15);
        Pen.fill;
      };
      string = this.cleanupFunc.asESDisplayString;
      lines = string.split($\n);
      lines.do { |line, i|
        while { max(0, width - 5) < (QtGUI.stringBounds(line, font).width) } {
          if (line.size == 1) {
            line = "";
          } {
            line = line[0..line.size-2];
          };
        };
        strTop = 30 + funcHeight + (i * 10);
        if (strTop < height) {
          Pen.stringAtPoint(line, (left+3.5)@(top+strTop), font, Color.gray(1.0, 0.4));
        };
      };
    };
    ^"Routine"
  }

  guiClass { ^ESRoutineClipEditView }

  defaultColor { ^Color.hsv(0.5, 0.4, 0.4) }
}