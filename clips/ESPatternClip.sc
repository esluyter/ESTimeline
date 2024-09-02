ESPatternClip : ESClip {
  var pattern, <randSeed, <>isSeeded;
  var player;
  var prTitle; // this is so we can draw instrument names without overhead

  storeArgs { ^[startTime, duration, offset, color, name, pattern, randSeed, isSeeded, mute]; }

  *new { |startTime, duration, offset = 0, color, name, pattern, randSeed, isSeeded = true, mute = false|
    ^super.new(startTime, duration, offset, color, name, mute: mute).init(pattern, randSeed, isSeeded);
  }

  init { |argPattern, argRandSeed, argIsSeeded|
    // copy pattern-specific args, with default (random) random seed
    pattern = argPattern;
    randSeed = argRandSeed ?? rand(2000000000);
    isSeeded = argIsSeeded;
  }

  startTime_ { |val, adjustOffset = false|
    super.startTime_(val, adjustOffset);
    if (adjustOffset) {
      drawClip.drawData = nil;
    };
  }

  endTime_ { |val|
    super.endTime_(val);
    drawClip.drawData = nil;
  }

  duration_ { |val|
    super.duration_(val);
    drawClip.drawData = nil;
  }

  offset_ { |val|
    super.offset_(val);
    drawClip.drawData = nil;
  }

  prHasOffset { ^true } // whether to show offset parameter for editing

  randSeed_ { |val|
    randSeed = val;
    drawClip.drawData = nil;
    this.changed(\randSeed, val);
  }

  pattern_ { |val|
    pattern = val;
    drawClip.drawData = nil;
    this.changed(\pattern, val);
  }

  pattern {
    // this is because we can supply a function to generate the pattern
    ^pattern.value;
  }

  patternString {
    ^pattern.asESDisplayString;
  }

  // helper method to return the actual pattern that will be played
  patternToPlay {
    // if this clip is seeded, return the seeded pattern.
    if (isSeeded) {
      ^Pseed(Pn(randSeed, 1), this.pattern);
    } {
      ^this.pattern;
    }
  }

  // helper method to generate stream
  patternStream {
    var stream = this.patternToPlay.asStream;
    var wait = if (offset.isPositive) {
      stream.fastForward(offset);
    } {
      -1 * offset;
    };
    if (wait != 0) {
      stream = Routine({ (dur: wait, restdummy: Rest()).yield; }) ++ stream;
    };
    ^stream;
  }

  // pattern specific stop method
  prStop {
    player.stop;
  }

  // pattern specific start method
  prStart { |startOffset = 0.0, clock|
    var stream = this.patternStream;
    var wait = stream.fastForward(startOffset);
    player = {
      wait.wait;
      if (track.timeline.useMixerChannel and: track.useMixerChannel) {
        player = track.timeline.mixerChannels[track.mixerChannelName].play(EventStreamPlayer(stream), (clock: clock));
      } {
        player = EventStreamPlayer(stream).play(clock);
      };
    }.fork(clock);
  }

  prTitle { |val|
    if (val.notNil) { prTitle = val };
    ^prTitle ?? { this.pattern.class.asString };
  }

  defaultColor { ^Color.hsv(0.4, 0.4, 0.4) }

  guiClass { ^ESPatternClipEditView }
  drawClass { ^ESDrawPatternClip }
}