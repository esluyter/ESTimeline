ESClip {
  var <startTime, <duration, >color, <offset;
  var <isPlaying = false;
  var playRout;

  storeArgs { ^[startTime, duration, color, offset] }

  *new { |startTime, duration, color, offset = 0|
    ^super.newCopyArgs(startTime, duration, color, offset);
  }

  startTime_ { |val, adjustOffset = false|
    val = max(val, 0);
    if (adjustOffset) {
      var delta;
      val = min(val, this.endTime);
      delta = val - startTime;
      offset = offset + delta;
      duration = duration - delta;
    };
    startTime = val;
    this.changed(\startTime, val);
  }

  endTime_ { |val|
    duration = max(val - startTime, 0);
    this.changed(\duration, duration);
  }

  duration_ { |val|
    duration = val;
    this.changed(\duration, val);
  }

  offset_ { |val|
    offset = val;
    this.changed(\offset, val);
  }

  stop {
    // stop the clip
    this.prStop;
    isPlaying = false;
    // in case of premature stop:
    playRout.stop;
  }

  play { |startOffset = 0.0, clock|
    // default to play on default TempoClock
    clock = clock ?? TempoClock.default;

    // stop if we're playing
    if (isPlaying) {
      this.stop;
    };

    // set isPlaying to true
    isPlaying = true;

    // play the clip on a new Routine on this clock
    playRout = {
      // start the clip from specified start offset
      this.prStart(startOffset, clock);

      // wait the appropriate time, then stop
      (duration - startOffset).wait;
      this.stop;
    }.fork(clock);
  }

  // draw this clip on a UserView using Pen
  draw { |left, top, width, height|
    Pen.color = this.color;
    Pen.addRect(Rect(left, top, width, height));
    Pen.fill;

    Pen.color = Color.gray(0.8, 0.5);
    Pen.addRect(Rect(left + width - 1, top, 1, height));
    Pen.fill;

    // if it's more than 5 pixels wide, call the prDraw function
    if (width > 5) {
      var title = this.prDraw(left, top, width, height);
      var font = Font("Helvetica", 14, true);
      if (left < 0) {
        width = width + left;
        left = 0;
      };
      while { max(0, width - 3.5) < (QtGUI.stringBounds(title, font).width) } {
        if (title.size == 1) {
          title = "";
        } {
          title = title[0..title.size-2];
        };
      };
      Pen.stringAtPoint(title, (left + 3.5)@(top + 2), font, Color.gray(1, 0.5));
    };
  }

  // override these in subclasses
  prStart { }
  prStop { }
  prDraw { ^"[empty]" }
  defaultColor { ^Color.hsv(0.5, 0.5, 0.5); }

  // helper methods
  endTime { ^startTime + duration }

  // getters
  color {
    ^color ?? { this.defaultColor }
  }

  rawColor {
    ^color
  }
}
