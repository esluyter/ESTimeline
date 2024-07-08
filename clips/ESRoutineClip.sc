ESRoutineClip : ESClip {
  var <routine, <>randSeed, <>isSeeded, <>addLatency,
  <>fastForward, // 0 - don't play from middle, 1 - fast forward from middle, 2 - start from beginning always
  <>prepFunc, <>cleanupFunc, <func, <>stopFunc;
  var player;

  storeArgs { ^[startTime, duration, offset, color, name, func, stopFunc, prepFunc, cleanupFunc, randSeed, isSeeded, addLatency, fastForward, mute] }

  *new { |startTime, duration, offset = 0, color, name, func, stopFunc, prepFunc, cleanupFunc, randSeed, isSeeded = true, addLatency = false, fastForward = 1, mute = false|
    ^super.new(startTime, duration, offset, color, name, mute: mute).init(func, stopFunc, prepFunc, cleanupFunc, randSeed, isSeeded, addLatency, fastForward);
  }

  init { |argFunc, argStopFunc, argPrepFunc, argCleanupFunc, argRandSeed, argIsSeeded, argAddLatency, argFastForward|
    //routine = ESRoutine(argFunc);
    func = argFunc;
    stopFunc = argStopFunc;
    prepFunc = argPrepFunc;
    cleanupFunc = argCleanupFunc;
    randSeed = argRandSeed ?? rand(2000000000);
    isSeeded = argIsSeeded;
    addLatency = argAddLatency;
    fastForward = argFastForward;
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
        stopFunc.value;
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

  prep {
    prepFunc.value;
  }

  cleanup {
    cleanupFunc.value;
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
        line = ESStringShortener.trim(line, width - 5, font);
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
      string = this.stopFunc.asESDisplayString;
      lines = string.split($\n);
      lines.do { |line, i|
        line = ESStringShortener.trim(line, width - 5, font);
        strTop = 30 + funcHeight + (i * 10);
        if (strTop < height) {
          Pen.stringAtPoint(line, (left+3.5)@(top+strTop), font, Color.gray(1.0, 0.4));
        };
      };
    };
    ^this.prTitle;
  }

  prTitle { ^"Routine" }

  prHasOffset { ^true } // whether to show offset parameter for editing

  guiClass { ^ESRoutineClipEditView }

  defaultColor { ^Color.hsv(0.5, 0.4, 0.4) }
}