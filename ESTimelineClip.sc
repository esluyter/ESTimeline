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

  play { |startOffset = 0.0, clock, maxDuration = inf|
    clock = timeline.prMakeClock;

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
      min(duration - startOffset, maxDuration).wait;
      this.stop;
    }.fork(clock);
  }

  prStop { |hard = false|
    timeline.stop(hard);
  }

  prStart { |startOffset = 0.0, clock|
    startOffset = startOffset + offset;

    timeline.play(startOffset, makeClock: false);
  }

  prDraw { |left, top, width, height, editingMode, clipLeft, clipWidth, selected|
    var tracks = timeline.tracks;
    var tratio = width / duration;
    var pratio = tratio.reciprocal;
    var rulerHeight = 0;
    var trackHeight;
    var thisLeft;
    var division;

    /*
    left is the left side of the entire clip
    clipLeft is the leftmost visible edge
    */
    //[left, width, clipLeft, clipWidth].postln;

    clipLeft = clipLeft ?? left;
    clipWidth = clipWidth ?? width;


    Pen.use {
      Pen.addRect(Rect(clipLeft, top, clipWidth, height));
      Pen.clip;

      rulerHeight = ((height + 400) / 60).clip(10, 20);
      Pen.addRect(Rect(left, top, width, rulerHeight));
      Pen.fillColor = if (useParentClock) { Color.gray(0.93) } { Color.white };
      Pen.strokeColor = Color.gray(0.3);
      Pen.width = 1;
      if (useParentClock) {
        Pen.fill;
      } {
        Pen.fillStroke;
      };
      //
      // if (useParentClock.not) {
      //   Pen.addRect(Rect(left, top + rulerHeight, width, 1));
      //   Pen.color = Color.gray(0.8);
      //   Pen.fill;
      // };

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

            // this stuff is all just for nested ESTimelineClips:
            var thisClipLeft = max(clipLeft, thisLeft);
            var thisClipWidth = thisWidth - (thisClipLeft - thisLeft);
            if ((thisClipLeft + thisClipWidth) > (clipLeft + clipWidth)) {
              thisClipWidth = (clipLeft + clipWidth) - thisClipLeft;
            };

            clip.draw(thisLeft, thisTop, thisWidth, thisHeight, false, thisClipLeft, thisClipWidth, selected, false);
          };
        };
      };


      if (timeline.isPlaying) {
        // sounding playhead in black
        thisLeft = ((timeline.soundingNow - offset) * tratio) + left;
        Pen.addRect(Rect(thisLeft, top, 2, height));
        Pen.color = Color.black;
        Pen.fill;

        // "scheduling playhead" in gray
        Pen.color = Color.gray(0.5, 0.5);
        thisLeft = ((timeline.now - offset) * tratio) + left;
        Pen.addRect(Rect(thisLeft, top, 2, height));
        Pen.fill;
      };

      Pen.addRect(Rect(left, top + rulerHeight + 1, width, height - rulerHeight - 1));
      Pen.strokeColor = if (timeline.useEnvir) { Color.gray(0.4) } { Color.gray(0.8) };
      Pen.width = 1;
      Pen.stroke;
    };
  }

  defaultColor { ^if (timeline.useEnvir) { Color.gray(0.96, 0.5) } { Color.clear  } }

  guiClass { ^ESTimelineClipEditView }

  prFree {
    timeline.free;
  }
}