ESClip {
  var <startTime, <duration, <offset, >color, <name, <comment;
  var <>track;
  var <isPlaying = false;
  var playRout;

  storeArgs { ^[startTime, duration, offset, color, name, comment] }

  *new { |startTime, duration, offset = 0, color, name, comment = ""|
    ^super.newCopyArgs(startTime, duration, offset, color, name, comment);
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

  name_ { |val|
    if (val.asString.size > 0) {
      name = val;
    } {
      name = nil;
    };
    this.changed(\name, val);
  }

  comment_ { |val|
    comment = val;
    this.changed(\comment, val);
  }

  prep {}
  cleanup {}

  stop { |hard = false|
    // stop the clip
    this.prStop(hard);
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
      // set "thisTimeline" environment var
      ~thisTimeline = track.timeline;

      // start the clip from specified start offset
      this.prStart(startOffset, clock);

      // wait the appropriate time, then stop
      (duration - startOffset).wait;
      this.stop;
    }.fork(clock);
  }

  // draw this clip on a UserView using Pen
  draw { |left, top, width, height, editingMode = false, clipLeft, clipWidth, selected = false, drawBorder = true| // these last two are for ESTimelineClips
    if (track.shouldPlay) {
      Pen.color = this.color(selected);
    } {
      Pen.color = Color.white.lighten(this.color(selected), 0.5);
    };
    Pen.strokeColor = Color.cyan;
    Pen.width = 2;
    Pen.addRect(Rect(left, top, width, height));
    if (selected and: drawBorder) { Pen.fillStroke } { Pen.fill };

    Pen.color = Color.gray(0.8, 0.5);
    Pen.addRect(Rect(left + width - 1, top, 1, height));
    Pen.fill;

    // if it's more than 5 pixels wide and high, call the prDraw function
    if ((width > 5) and: (height > 10)) {
      var font = Font("Helvetica", 14, true);
      var title;
      try {
        if (track.timeline.useEnvir) {
          track.timeline.envir.use {
            title = this.prDraw(left, top, width, height, editingMode, clipLeft, clipWidth, selected, drawBorder);
          }
        } {
          title = this.prDraw(left, top, width, height, editingMode, clipLeft, clipWidth, selected, drawBorder);
        };
      } {
        title = ""
      };

      if (name.notNil) { title = name.asCompileString ++ " (" ++ title ++ ")" };

      if (left < 0) {
        width = width + left;
        left = 0;
      };
      if (clipLeft.notNil and: { left < clipLeft }) {
        width = width - (clipLeft - left);
        left = clipLeft;
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

  free {
    this.cleanup;
    this.prFree;
    this.release;
  }

  // override these in subclasses
  prFree { }
  prStart { }
  prStop { }
  prDraw { |left, top, width, height|
    var lines = comment.split($\n);
    var font = Font.sansSerif(14);
    var strTop;
    if (left < 0) {
      width = width + left;
      left = 0;
    };
    lines.do { |line, i|
      var thisFont = if (i > 0) { font } { font.copy.size_(17) };
      while { max(0, width - 5) < (QtGUI.stringBounds(line, thisFont).width) } {
        if (line.size == 1) {
          line = "";
        } {
          line = line[0..line.size-2];
        };
      };
      strTop = (2+(i * 16));
      if (strTop < height) {
        Pen.stringAtPoint(line, (left+3.5)@(strTop + top), thisFont, Color.gray(0.0, 0.7));
      };
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
  color { |selected = false|
    var ret = color ?? { this.defaultColor };
    if (selected) {
      ret = Color.white.lighten(ret, 0.5);
    };
    ^ret;
  }

  rawColor {
    ^color
  }

  duplicate {
    ^this.asCompileString.interpret;
  }

  index {
    ^track.clips.indexOf(this);
  }

  guiClass { ^ESClipEditView }

  prHover { |x, y, hoverTime, left, top, width, height| }
  prHoverLeave {}
  prMouseMove { |x, y, xDelta, yDelta, left, top, width, height| }
  prMouseDown { |x, y, mods, buttNum, clickCount, left, top, width, height| }
}
