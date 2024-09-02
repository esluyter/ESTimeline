ESTimelineClip : ESClip {
  var <timeline, <useParentClock;

  useParentClock_ { |val|
    useParentClock = val;
    timeline.changed(\useParentClock, val);
  }

  storeArgs { ^[startTime, duration, timeline, useParentClock, color, offset] }

  *new { |startTime, duration, timeline, useParentClock = true, color, offset = 0|
    ^super.new(startTime, duration, color).init(timeline, useParentClock, offset);
  }

  init { |argTimeline, argUseParentClock, argOffset|
    timeline = argTimeline;
    useParentClock = argUseParentClock;
    offset = argOffset;
    timeline.parentClip = this;
    timeline.addDependant(this);
  }

  update { |argTimeline, what, val|
    this.changed(\timeline, [what, val]);
  }

  play { |startOffset = 0.0, clock|
    // default to play on default TempoClock
    clock = clock ?? TempoClock.default;

    if (useParentClock.not) {
      clock = timeline.clock;
    };

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

  prStop {
    timeline.stop;
  }

  prStart { |startOffset = 0.0, clock|
    startOffset = startOffset + offset;

    if (useParentClock) {
      timeline.play(startOffset, clock);
    } {
      timeline.play(startOffset);
    };
  }

  prDraw { |left, top, width, height, editingMode|
    var tracks = timeline.tracks;
    var tratio = width / duration;
    var pratio = tratio.reciprocal;
    var rulerHeight = 0;
    var trackHeight;
    var thisLeft;
    var division;

    Pen.use {
      Pen.addRect(Rect(left, top, width, height));
      Pen.clip;

      rulerHeight = ((height + 400) / 60).clip(10, 20);
      Pen.addRect(Rect(left + 1, top + 1, width - 2, rulerHeight));
      Pen.color = if (useParentClock) { Color.gray(0.93) } { Color.white };
      Pen.fill;

      if (useParentClock.not) {
        Pen.addRect(Rect(left, top + rulerHeight, width, 1));
        Pen.color = Color.gray(0.8);
        Pen.fill;
      };

      division = (60 / (width / duration)).ceil;
      Pen.color = if (useParentClock) { Color.gray(0.3, 0.5) } { Color.gray(0.3) };
      (offset + duration + 1).asInteger.do { |i|
        if (i % division == 0) {
          if (i >= offset) {
            thisLeft = ((i - offset) * tratio) + left;
            Pen.addRect(Rect(thisLeft, top + 1, 1, rulerHeight));
            Pen.fill;
            Pen.stringAtPoint(i.asString, (thisLeft + 3)@(top + 1), Font("Courier New", rulerHeight * (16/20)));
          };
        };
      };

      trackHeight = (height - rulerHeight) / tracks.size;
      tracks.do { |track, i|
        track.clips.do { |clip|
          if ((clip.endTime > offset) and: (clip.startTime < (offset + duration))) {
            var thisLeft = ((clip.startTime - offset) * tratio) + left;
            var thisWidth = (clip.duration * tratio);
            var thisTop = top + rulerHeight + (trackHeight * i) + 2;
            var thisHeight = trackHeight - 3;
            clip.draw(thisLeft, thisTop, thisWidth, thisHeight);
          };
        };
      };


      if (timeline.isPlaying) {
        // sounding playhead in black
        thisLeft = ((timeline.soundingNow - offset) * tratio) + left;
        Pen.addRect(Rect(thisLeft, 0, 2, height));
        Pen.color = Color.black;
        Pen.fill;

        // "scheduling playhead" in gray
        Pen.color = Color.gray(0.5, 0.5);
        thisLeft = ((timeline.now - offset) * tratio) + left;
        Pen.addRect(Rect(thisLeft, 0, 2, height));
        Pen.fill;
      };

      Pen.addRect(Rect(left, top + rulerHeight, width, height - rulerHeight));
      Pen.strokeColor = if (timeline.useEnvir) { Color.gray(0.4) } { Color.gray(0.8) };
      Pen.width = 1;
      Pen.stroke;
    };
  }

  defaultColor { ^if (timeline.useEnvir) { Color.gray(0.97) } { Color.clear  } }

  guiClass { ^ESTimelineClipEditView }

  prFree {
    timeline.free;
  }
}