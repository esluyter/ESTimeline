ESRoutineClip : ESClip {
  var <routine, <>randSeed, <>isSeeded, <>addLatency,
  <>fastForward, // 0 - don't play from middle, 1 - fast forward from middle, 2 - start from beginning always
  <>cleanupFunc, <func;
  var player;

  storeArgs { ^[startTime, duration, func, randSeed, isSeeded, addLatency, fastForward, cleanupFunc, color, offset] }

  *new { |startTime, duration, func, randSeed, isSeeded = true, addLatency = false, fastForward = 1, cleanupFunc, color, offset = 0|
    ^super.new(startTime, duration, color).init(func, randSeed, isSeeded, addLatency, fastForward, cleanupFunc, offset);
  }

  init { |argFunc, argRandSeed, argIsSeeded, argAddLatency, argFastForward, argCleanupFunc, argOffset|
    //routine = ESRoutine(argFunc);
    func = argFunc;
    randSeed = argRandSeed ?? rand(2000000000);
    isSeeded = argIsSeeded;
    addLatency = argAddLatency;
    fastForward = argFastForward;
    cleanupFunc = argCleanupFunc;
    offset = argOffset;
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
    //routine = ESRoutine(func);
  }

  prDraw { |left, top, width, height|
    if ((height > 30) and: (width > 30)) {
      var string = this.funcString;
      var lines = string.split($\n);
      var font = Font.monospace(10);
      var funcHeight = lines.size * 10;
      lines.do { |line, i|
        while { max(0, width - 5) < (QtGUI.stringBounds(line, font).width) } {
          if (line.size == 1) {
            line = "";
          } {
            line = line[0..line.size-2];
          };
        };
        Pen.stringAtPoint(line, (left+3.5)@(top+20+(i * 10)), font, Color.gray(1.0, 0.6));
      };
      Pen.addRect(Rect(left, top + 25 + funcHeight, width / 2, 1));
      Pen.color = Color.gray(1.0, 0.15);
      Pen.fill;
      string = this.cleanupFuncString;
      lines = string.split($\n);
      lines.do { |line, i|
        while { max(0, width - 5) < (QtGUI.stringBounds(line, font).width) } {
          if (line.size == 1) {
            line = "";
          } {
            line = line[0..line.size-2];
          };
        };
        Pen.stringAtPoint(line, (left+3.5)@(top+30+funcHeight+(i * 10)), font, Color.gray(1.0, 0.4));
      };
    };
    ^"Routine"
  }

  funcString {
    if (func.isFunction) {
      var cs = func.asCompileString;
      if (cs.size == 2) { ^"" };
      ^cs[1..cs.size-2];
    };
    ^"";
  }

  cleanupFuncString {
    if (cleanupFunc.isFunction) {
      var cs = cleanupFunc.asCompileString;
      if (cs.size == 2) { ^"" };
      ^cs[1..cs.size-2];
    };
    ^"";
  }

  guiClass { ^ESRoutineClipEditView }
}