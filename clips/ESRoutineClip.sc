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

  prTitle { ^"Routine" }

  prHasOffset { ^true } // whether to show offset parameter for editing

  guiClass { ^ESRoutineClipEditView }
  drawClass { ^ESDrawRoutineClip }

  defaultColor { ^Color.hsv(0.5, 0.4, 0.4) }
}