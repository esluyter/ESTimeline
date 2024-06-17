ESClip {
  var <startTime, <duration, >color, <offset, <comment;
  var <>track;
  var <isPlaying = false;
  var playRout;

  storeArgs { ^[startTime, duration, color, offset, comment] }

  *new { |startTime, duration, color, offset = 0, comment = ""|
    ^super.newCopyArgs(startTime, duration, color, offset, comment);
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

  comment_ { |val|
    comment = val;
    this.changed(\comment, val);
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
  draw { |left, top, width, height, editingMode|
    if (track.shouldPlay) {
      Pen.color = this.color;
    } {
      Pen.color = Color.white.lighten(this.color, 0.5);
    };
    Pen.addRect(Rect(left, top, width, height));
    Pen.fill;

    Pen.color = Color.gray(0.8, 0.5);
    Pen.addRect(Rect(left + width - 1, top, 1, height));
    Pen.fill;

    // if it's more than 5 pixels wide, call the prDraw function
    if (width > 5) {
      var font = Font("Helvetica", 14, true);
      var title;
      if (track.timeline.useEnvir) {
        track.timeline.envir.use {
          title = this.prDraw(left, top, width, height, editingMode);
        }
      } {
        title = this.prDraw(left, top, width, height, editingMode);
      };

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
      Pen.stringAtPoint(title, (left + 3.5)@(top + 2), font, Color.gray(1, 0.6));
    };
  }

  // override these in subclasses
  prStart { }
  prStop { }
  prDraw { |left, top, width, height|
    var lines = comment.split($\n);
    var font = Font.sansSerif(14);
    if (left < 0) {
      width = width + left;
      left = 0;
    };
    lines.do { |line, i|
      while { max(0, width - 5) < (QtGUI.stringBounds(line, font).width) } {
        if (line.size == 1) {
          line = "";
        } {
          line = line[0..line.size-2];
        };
      };
      Pen.stringAtPoint(line, (left+3.5)@(top+2+(i * 16)), if (i > 0) { font } { font.copy.size_(17) }, Color.gray(0.0, 0.7));
    };
    Pen.color = Color.gray(0.7);
    Pen.addRect(left, top, width, height);
    Pen.stroke;
    ^""
  }
  defaultColor { ^Color.gray(1); }

  // helper methods
  endTime { ^startTime + duration }

  // getters
  color {
    ^color ?? { this.defaultColor }
  }

  rawColor {
    ^color
  }

  guiClass { ^ESClipEditView }

  prHover { |x, y, hoverTime, left, top, width, height| }
  prHoverLeave {}
  prMouseMove { |x, y, xDelta, yDelta, left, top, width, height| }
  prMouseDown { |x, y, mods, buttNum, clickCount, left, top, width, height| }
}
