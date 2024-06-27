ESRulerView : UserView {
  var <>timeline, <>timelineView;
  var clickPoint, clickTime, originalDuration;
  var <playheadView, playheadRout;

  *new { |parent, bounds, timeline, timelineView|
    ^super.new(parent, bounds).init(timeline, timelineView);
  }

  init { |argtimeline, argtimelineView|
    var width = this.bounds.width;
    var height = this.bounds.height;

    timelineView = argtimelineView;
    timeline = argtimeline;

    this.drawFunc_({
      var division = (60 / (this.bounds.width / this.duration)).ceil;
      var timeSelection = timelineView.timeSelection;

      if (timeSelection.notNil) {
        var left = this.absoluteTimeToPixels(timeSelection[0]);
        var width = this.relativeTimeToPixels(timeSelection[1] - timeSelection[0]);
        Pen.addRect(Rect(left, 0, width, this.bounds.height));
        Pen.color = Color.gray(0.5, 0.2);
        Pen.fill;
        Pen.addRect(Rect(left - 1, 0, 1, this.bounds.height));
        Pen.addRect(Rect(left + width, 0, 1, this.bounds.height));
        Pen.color = Color.gray(0.6);
        Pen.fill;
      };

      Pen.use {
        Pen.color = Color.black;
        (this.startTime + this.duration + 1).asInteger.do { |i|
          if (i % division == 0) {
            var left = this.absoluteTimeToPixels(i);
            Pen.addRect(Rect(left, 0, 1, 20));
            Pen.fill;
            Pen.stringAtPoint(i.asString, (left + 3)@0, Font("Courier New", 16));
          }
        };
      };

      if (timeline.parentClip.notNil) {
        if (timeline.parentClip.offset > this.startTime) {
          Pen.addRect(Rect(0, 0, this.absoluteTimeToPixels(timeline.parentClip.offset), this.bounds.height));
          Pen.color = (Color.gray(0.5, 0.5));
          Pen.fill;
        };
        if (timeline.parentClip.offset + timeline.parentClip.duration < this.endTime) {
          var left = this.absoluteTimeToPixels(timeline.parentClip.offset + timeline.parentClip.duration);
          Pen.addRect(Rect(left, 0, this.bounds.width, this.bounds.height));
          Pen.color = (Color.gray(0.5, 0.5));
          Pen.fill;
        };
      };

      // draw clip guides
      if (timelineView.drawClipGuides) {
        Pen.addRect(Rect(this.absoluteTimeToPixels(timelineView.hoverClip.startTime), 0, 1, this.bounds.height));
        Pen.addRect(Rect(this.absoluteTimeToPixels(timelineView.hoverClip.endTime), 0, 1, this.bounds.height));
        Pen.color = Color.gray(0.6);
        Pen.fill;
      };
    }).mouseWheelAction_({ |view, x, y, mods, xDelta, yDelta|
      var xTime = timelineView.pixelsToAbsoluteTime(x);
      timelineView.duration = timelineView.duration * (-1 * yDelta).linexp(-100, 100, 0.5, 2, nil);
      timelineView.startTime = xTime - timelineView.pixelsToRelativeTime(x);
      timelineView.startTime = timelineView.startTime + (xDelta * timelineView.duration * -0.002);
    }).mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
      clickPoint = x@y;
      clickTime = this.pixelsToAbsoluteTime(x);
      originalDuration = timelineView.duration;
    }).mouseUpAction_({ |view, x, y, mods|
      // if the mouse didn't move during the click, move the playhead to the click point:
      if (clickPoint == (x@y)) {
        timeline.now = clickTime;
      };

      clickPoint = nil;
      clickTime = nil;
      originalDuration = nil;
      //this.refresh;
    }).mouseMoveAction_({ |view, x, y, mods|
      var yDelta = y - clickPoint.y;
      var xDelta = x - clickPoint.x;
      // drag timeline
      if (mods.isAlt) { // hold option to zoom in opposite direction
        yDelta = yDelta.neg;
      };
      timelineView.duration = (originalDuration * (-1 * yDelta).linexp(-100, 100, 0.5, 2, nil));
      timelineView.startTime = (clickTime - this.pixelsToRelativeTime(clickPoint.x));
      timelineView.startTime = (xDelta.linlin(0, this.bounds.width, timelineView.startTime, timelineView.startTime - timelineView.duration, nil));
      //this.refresh;
    }).keyDownAction_({ |view, char, mods, unicode, keycode, key|
      timelineView.keyDownAction.(view, char, mods, unicode, keycode, key);
    });

    playheadView = UserView(this, this.bounds.copy.origin_(0@0))
    .acceptsMouse_(false)
    .drawFunc_({
      var left;
      Pen.use {
        // sounding playhead in black
        left = this.absoluteTimeToPixels(timeline.soundingNow);
        Pen.addRect(Rect(left, 0, 2, height));
        Pen.color = Color.black;
        Pen.fill;

        if (timeline.isPlaying) {
          // "scheduling playhead" in gray
          Pen.color = Color.gray(0.5, 0.5);
          left = this.absoluteTimeToPixels(timeline.now);
          Pen.addRect(Rect(left, 0, 2, height));
          Pen.fill;
        };
      };
    });
  }

  startTime { ^timelineView.startTime }
  duration { ^timelineView.duration }
  endTime { ^timelineView.endTime }

  // helper methods:
  relativeTimeToPixels { |time| ^timelineView.relativeTimeToPixels(time) }
  absoluteTimeToPixels { |time| ^timelineView.absoluteTimeToPixels(time) }
  pixelsToRelativeTime { |pixels| ^timelineView.pixelsToRelativeTime(pixels) }
  pixelsToAbsoluteTime { |pixels| ^timelineView.pixelsToAbsoluteTime(pixels) }
}