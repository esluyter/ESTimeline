ESPatternClip : ESClip {
  var pattern, <randSeed, <>isSeeded;
  var player;
  var drawData;
  var prTitle;

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
      drawData = nil;
    };
  }

  endTime_ { |val|
    super.endTime_(val);
    drawData = nil;
  }

  duration_ { |val|
    super.duration_(val);
    drawData = nil;
  }

  offset_ { |val|
    super.offset_(val);
    drawData = nil;
  }

  prHasOffset { ^true } // whether to show offset parameter for editing

  randSeed_ { |val|
    randSeed = val;
    drawData = nil;
    this.changed(\randSeed, val);
  }

  pattern_ { |val|
    pattern = val;
    drawData = nil;
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
      player = EventStreamPlayer(stream).play(clock);
    }.fork(clock);
  }

  // data to be drawn on the clip
  drawData {
    var stream, t;

    // return cached data if it exists
    if (drawData.asArray.size > 0) {
      ^drawData;
    };

    // otherwise calculate it
    stream = this.patternStream;
    drawData = [];
    t = 0.0;
    // keep adding events until we're past the duration (or the stream has run out)
    while { t < duration } {
      // use default Event as the proto event (we need this to be sure of calculating the keys later)
      var event = stream.next(Event.default);
      if (event.notNil) {
        // if the stream has not run out:
        var simpleEvent = event.use {
          (
            instrument: event.instrument,
            freq: event.freq,
            amp: event.amp,
            sustain: event.sustain,
            dur: event.dur,
            // because it doesn't work to specify the isRest parameter directly, as
            // mentioned in part 0
            restdummy: if (event.isRest) { Rest() } { 1 }
          )
        };
        drawData = drawData.add(simpleEvent);
        t = t + simpleEvent.dur;
      } {
        // if the stream has run out:
        t = inf;
      };
    };
    ^drawData;
  }

  // draw the pattern data onto a UserView using Pen
  prDraw { |left, top, width, height|
    var t = 0.0;
    var instrument = Set(8);

    /*
    var string = this.patternString;
    var lines = string.split($\n);
    var font = Font.monospace(10);
    lines.do { |line, i|
      line = ESStringShortener.trim(line, width - 5, font);
      Pen.stringAtPoint(line, (left+3.5)@(top+20+(i * 10)), font, Color.gray(1.0, 0.4));
    };
    */

    try {
      this.drawData.do { |event|
        if (event.isRest.not) {
          var x = left + (t * width / duration);
          var eventHeight = 2;
          event.freq.asArray.do { |freq, i|
            var eventWidth, y, amp;
            amp = event.amp.asArray.wrapAt(i);
            if (amp.isNumber.not) { amp = 0.1 };
            if (freq.isNumber.not) { freq = 500 };
            eventWidth = event.sustain.asArray.wrapAt(i) * width / duration;
            y = freq.explin(20, 20000, top + height, top);
            Pen.color = Color.gray(1, amp.ampdb.linexp(-60.0, 0.0, 0.05, 1.0));
            Pen.addRect(Rect(x, y, eventWidth, eventHeight));
            Pen.fill;
          };
          instrument.add(event.instrument.asString);
        };
        t = t + event.dur;
      };
      // return the title of the clip
      ^this.prTitle(instrument.asArray.join(" / ") ++ ": " ++ this.pattern.class);
    } {
      ^this.prTitle;
    };
  }

  prTitle { |val|
    if (val.notNil) { prTitle = val };
    ^prTitle ?? { this.pattern.class.asString };
  }

  defaultColor { ^Color.hsv(0.4, 0.55, 0.5) }

  guiClass { ^ESPatternClipEditView }
}