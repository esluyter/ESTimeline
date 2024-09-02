ESTimelineView : UserView {
  var <timeline;
  var <trackViews, <playheadView, playheadRout;
  var <dragView, <leftGuideView, <rightGuideView;
  var <startTime, <duration;
  var <trackHeight;
  var clickPoint, clickTime, scrolling = false, originalDuration;
  var hoverClip, hoverCode, hoverClipStartTime, hoverClipOffset;
  var hoverTime = 0, hoverTrack = 0, hoverClipIndex = 0;
  var duplicatedClip;

  var <editingMode = false;

  editingMode_ { |val| editingMode = val; this.changed(\editingMode, val) }


  *new { |parent, bounds, timeline, startTime = -2.0, duration = 50.0|
    ^super.new(parent, bounds).init(timeline, startTime, duration);
  }

  init { |argtimeline, argstartTime, argduration|
    var width = this.bounds.width;
    var height = this.bounds.height;

    startTime = argstartTime;
    duration = argduration;

    timeline = argtimeline;

    this.makeTrackViews;

    this.setContextMenuActions(
      Menu(
        MenuAction("Add Comment (C)", { timeline.tracks[hoverTrack].addClip(ESClip(hoverTime, 5)) }),
        MenuAction("Add Synth Clip (S)", { timeline.tracks[hoverTrack].addClip(ESSynthClip(hoverTime, 5, \default)) }),
        MenuAction("Add Pattern Clip (P)", { timeline.tracks[hoverTrack].addClip(ESPatternClip(hoverTime, 5, {Pbind()})) }),
        MenuAction("Add Routine Clip (R)", { timeline.tracks[hoverTrack].addClip(ESRoutineClip(hoverTime, 5, {})) }),
        MenuAction("Add Env Clip (E)", { timeline.tracks[hoverTrack].addClip(ESEnvClip(hoverTime, 5, Env([0, 1, 0], [2.5, 2.5], \sin))); }),
      ).title_("Add Clip"),
      MenuAction("Edit Clip (e)", { hoverClip.guiClass.new(hoverClip, timeline) }),
      MenuAction("Split Clip (s)", { if (hoverClip.notNil) { timeline.tracks[hoverTrack].splitClip(hoverClipIndex, hoverTime) } }),
      MenuAction("Delete Clip (⌫)", { if (hoverClipIndex.notNil) { timeline.tracks[hoverTrack].removeClip(hoverClipIndex) } }),
      MenuAction.separator(""),
      MenuAction("Add Track Before (Cmd+T)", { timeline.addTrack(hoverTrack) }),
      MenuAction("Add Track After (Cmd+t)", { timeline.addTrack(hoverTrack + 1) }),
      MenuAction("Delete Track (Cmd+⌫)", { timeline.removeTrack(hoverTrack) }),
    );

    this.drawFunc_({

    }).mouseWheelAction_({ |view, x, y, mods, xDelta, yDelta|
      var xTime = view.pixelsToAbsoluteTime(x);
      if (mods.isCmd) { view.duration = view.duration * (-1 * yDelta).linexp(-100, 100, 0.5, 2, nil); };
      if (mods.isAlt) {
        var ratio = yDelta.linexp(-100, 100, 0.5, 2, nil);
        var diff = y - this.parent.visibleOrigin.y;
        var oldHeight = this.bounds.height;
        this.bounds = this.bounds.height_((oldHeight * ratio).max(this.parent.bounds.height).min(this.parent.bounds.height * timeline.tracks.size));
        ratio = this.bounds.height / oldHeight;
        this.makeTrackViews;
        this.parent.visibleOrigin_(0@((this.parent.visibleOrigin.y + diff) * ratio - diff));
      };
      view.startTime = xTime - view.pixelsToRelativeTime(x);
      view.startTime = view.startTime + (xDelta * view.duration * -0.002);
      dragView.visible_(false);

      if (mods.isCmd or: mods.isAlt) {
        true;
      } {
        false;
      };
    }).mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
      clickPoint = x@y;
      clickTime = this.pixelsToAbsoluteTime(x);
      originalDuration = duration;
      if (hoverClip.notNil) {
        hoverClipStartTime = hoverClip.startTime;
        hoverClipOffset = hoverClip.offset;
      };

      if ((clickCount > 1) and: hoverClip.notNil) {
        // edit the clip on double click
        hoverClip.guiClass.new(hoverClip, timeline);
      };

      // alt to duplicate clip
      if (mods.isAlt) {
        duplicatedClip = hoverClip.deepCopy;
      };

      if (hoverClip.notNil and: editingMode) {
        // again, not the best:
        hoverClip.prMouseDown(x, y - (trackHeight * hoverClip.track.index), mods, buttNum, clickCount, *this.clipBounds(hoverClip))
      };
    }).mouseUpAction_({ |view, x, y, mods|
      // if the mouse didn't move during the click, move the playhead to the click point:
      if (clickPoint == (x@y)) {
        if (timeline.isPlaying.not) {
          timeline.now = clickTime;
        };
      };

      duplicatedClip = nil;

      [leftGuideView, rightGuideView].do(_.visible_(false));

      clickPoint = nil;
      clickTime = nil;
      scrolling = false;
      originalDuration = nil;
      hoverClipStartTime = nil;

      timeline.addUndoPoint;
      this.refresh;
    }).mouseMoveAction_({ |view, x, y, mods|
      var yDelta = y - clickPoint.y;
      var xDelta = x - clickPoint.x;

      if (editingMode.not) {
        switch (hoverCode)
        {1} { // drag left edge
          hoverClip.startTime_(this.pixelsToAbsoluteTime(x), true);
          dragView.bounds_(dragView.bounds.left_(this.absoluteTimeToPixels(hoverClip.startTime)));
        }
        {2} { // drag right edge
          hoverClip.endTime = this.pixelsToAbsoluteTime(x);
          dragView.bounds_(dragView.bounds.left_(this.absoluteTimeToPixels(hoverClip.endTime) - 2));
        }
        {0} { // drag clip
          if (mods.isCmd) {
            hoverClip.offset = hoverClipOffset - this.pixelsToRelativeTime(xDelta);
          } {
            var currentHoverTrack = this.trackAtY(y);

            if (duplicatedClip.notNil) {
              timeline.tracks[hoverTrack].addClip(duplicatedClip);
              hoverClip = duplicatedClip;
              hoverClipIndex = timeline.tracks[hoverTrack].clips.indexOf(duplicatedClip);
              duplicatedClip = nil;
            };

            hoverClip.startTime = hoverClipStartTime + this.pixelsToRelativeTime(xDelta);
            if (currentHoverTrack != hoverTrack) {
              timeline.tracks[hoverTrack].removeClip(timeline.tracks[hoverTrack].clips.indexOf(hoverClip));
              timeline.tracks[currentHoverTrack].addClip(hoverClip);
              hoverTrack = currentHoverTrack;
              hoverClipIndex = timeline.tracks[hoverTrack].clips.indexOf(hoverClip);
            };
          };
        };

        // draw clip guides
        if (hoverCode.notNil) {
          leftGuideView.bounds_(leftGuideView.bounds.left_(this.absoluteTimeToPixels(hoverClip.startTime)));
          rightGuideView.bounds_(rightGuideView.bounds.left_(this.absoluteTimeToPixels(hoverClip.endTime)));
          [leftGuideView, rightGuideView].do(_.visible_(true));
        } {
          [leftGuideView, rightGuideView].do(_.visible_(false));
        };
      } {  // if editingMode
        if (hoverClip.notNil) {
          //                        not the best
          hoverClip.prMouseMove(x, y - (trackHeight * hoverClip.track.index), xDelta, yDelta, *this.clipBounds(hoverClip));
        };
      };// end editingMode
    }).mouseOverAction_({ |view, x, y|
      var i, j;
      var oldHoverClip = hoverClip;
      # hoverClip, i, j, hoverCode = this.clipAtPoint(x@y);
      hoverTrack = i;
      hoverClipIndex = j;
      hoverTime = this.pixelsToAbsoluteTime(x);

      if (editingMode.not) {
        switch (hoverCode)
        {1} { // left edge
          dragView.bounds_(dragView.bounds.origin_(this.absoluteTimeToPixels(hoverClip.startTime)@(i * trackHeight)));
          dragView.visible_(true);
        }
        {2} { // right edge
          dragView.bounds_(dragView.bounds.origin_((this.absoluteTimeToPixels(hoverClip.endTime) - 2)@(i * trackHeight)));
          dragView.visible_(true);
        }
        { // default
          dragView.visible_(false);
        };
      } {
        if (oldHoverClip.notNil and: (oldHoverClip != hoverClip)) {
          oldHoverClip.prHoverLeave;
        };
        if (hoverClip.notNil) {
          //                  this is bad:
          hoverClip.prHover(x, y - (trackHeight * hoverClip.track.index), hoverTime, *this.clipBounds(hoverClip));
        };
      };
    }).keyDownAction_({ |view, char, mods, unicode, keycode, key|
      //key.postln;
      // space is play
      if (char == $ ) { timeline.togglePlay };
      // s - split clip
      if (char == $s) { if (hoverClip.notNil) { timeline.tracks[hoverTrack].splitClip(hoverClipIndex, hoverTime) } };
      if (char == $C) {
        timeline.tracks[hoverTrack].addClip(ESClip(hoverTime, 5));
      };
      if (char == $S) {
        timeline.tracks[hoverTrack].addClip(ESSynthClip(hoverTime, 5, \default));
      };
      if (char == $P) {
        timeline.tracks[hoverTrack].addClip(ESPatternClip(hoverTime, 5, {Pbind()}));
      };
      if (char == $R) {
        timeline.tracks[hoverTrack].addClip(ESRoutineClip(hoverTime, 5, {}));
      };
      if (char == $E) {
        timeline.tracks[hoverTrack].addClip(ESEnvClip(hoverTime, 5, Env([0, 1, 0], [2.5, 2.5], \sin)));
      };
      if (char == $e) {
        hoverClip.guiClass.new(hoverClip, timeline);
      };
      // cmd-E toggles editing mode
      if ((key == 69) and: mods.isCmd) {
        this.editingMode = editingMode.not
      };
      // delete - remove clip, cmd - remove track
      if (key == 16777219) {
        if (mods.isCmd) {
          timeline.removeTrack(hoverTrack);
        } {
          if (hoverClipIndex.notNil) {
            timeline.tracks[hoverTrack].removeClip(hoverClipIndex);
            hoverClip = nil;
            hoverClipIndex = nil;
          };
        };
      };
      // cmd-t new track (shift before, default after)
      if (mods.isCmd and: (key == 84)) {
        if (mods.isShift) {
          timeline.addTrack(hoverTrack);
        } {
          timeline.addTrack(hoverTrack + 1);
        };
      };

      // cmd-z undo, cmd-shift-z redo
      if (mods.isCmd and: (key == 90)) {
        if (mods.isShift) {
          timeline.redo;
        } {
          timeline.undo;
        };
      } {
        timeline.addUndoPoint;
      };
    });

    // call update method on changed
    timeline.addDependant(this);
    this.onClose = { timeline.removeDependant(this) };
  }

  makeTrackViews {
    // call this when number of tracks changes
    var width = this.bounds.width;
    var height = this.bounds.height;

    trackViews.do(_.remove);
    trackHeight = height / timeline.tracks.size;
    trackViews = timeline.tracks.collect { |track, i|
      var top = i * trackHeight;
      ESTrackView(this, Rect(0, top, width, trackHeight), track)
    };

    [playheadView, dragView, leftGuideView, rightGuideView].do(_.remove);
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
    dragView = View(this, Rect(0, 0, 2, trackHeight)).visible_(false).background_(Color.red).acceptsMouse_(false);
    leftGuideView = View(this, Rect(0, 0, 1, height)).visible_(false).background_(Color.gray(0.6)).acceptsMouse_(false);
    rightGuideView = View(this, Rect(0, 0, 1, height)).visible_(false).background_(Color.gray(0.6)).acceptsMouse_(false);

    this.changed(\makeTrackViews);
  }

  // called when the timeline is changed
  update { |argtimeline, what, value|
    if (what == \isPlaying) {
      if (value) {
        var waitTime = 30.reciprocal; // 30 fps
        playheadRout.stop; // just to make sure
        playheadRout = {
          inf.do { |i|
            playheadView.refresh;
            if (timeline.optimizeView.not) {
              this.refresh;
            };
            waitTime.wait;
          };
        }.fork(AppClock) // lower priority clock for GUI updates
      } {
        playheadRout.stop;
        playheadView.refresh;
      };
    };
  }

  clipAtPoint { |point|
    timeline.tracks.do { |track, i|
      var top = i * trackHeight;
      if (point.y.inRange(top, top + trackHeight)) {
        track.clips.do { |clip, j|
          // if clip within bounds...
          if ((clip.startTime < this.endTime) and: (clip.endTime > this.startTime)) {
            var left = this.absoluteTimeToPixels(clip.startTime);
            var width = this.relativeTimeToPixels(clip.duration);
            // if our point is within the clip's bounds...
            if (point.x.inRange(left, left + width)) {
              if ((point.x - left) < 3) { ^[clip, i, j, 1] }; // code for mouse over left edge
              if (((left + width) - point.x) < 3) { ^[clip, i, j, 2] }; // code for mouse over right edge
              ^[clip, i, j, 0];
            };
          };
        };
        ^[nil, i, nil, nil];
      };
    };
    ^[nil, 0, nil, nil];
  }

  trackAtY { |y|
    timeline.tracks.do { |track, i|
      var bottom = (i + 1) * trackHeight;
      if (y < bottom) {
        ^i;
      };
    };
    ^(timeline.tracks.size - 1);
  }

  // helper methods:
  relativeTimeToPixels { |time| ^(time / duration) * this.bounds.width }
  absoluteTimeToPixels { |clipStartTime| ^this.relativeTimeToPixels(clipStartTime - startTime) }
  pixelsToRelativeTime { |pixels| ^(pixels / this.bounds.width) * duration }
  pixelsToAbsoluteTime { |pixels| ^this.pixelsToRelativeTime(pixels) + startTime }

  clipBounds { |clip|
    var left = this.absoluteTimeToPixels(clip.startTime);
    var width = this.relativeTimeToPixels(clip.duration);
    ^[left, 3, width, trackHeight - 4, editingMode];
  }

  startTime_ { |val|
    startTime = val;
    this.changed(\startTime, val);
    this.refresh;
  }

  duration_ { |val|
    duration = val;
    this.changed(\duration, val);
    this.refresh;
  }

  endTime {
    ^startTime + duration;
  }
}