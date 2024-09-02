ESTimelineView : UserView {
  var <timeline;
  var <trackViews, <playheadView;
  var <dragView, <leftGuideView, <rightGuideView;
  var <startTime, <duration;
  var <trackHeight, <heightRatio = 1;
  var clickPoint, clickTime, scrolling = false, originalDuration;
  var <hoverClip, <hoverCode, hoverClipStartTime, hoverClipEndTime, hoverClipOffset;
  var <hoverEnv;
  var hoverTime = 0, <>hoverTrack;
  var duplicatedClips, <timeSelection, clipSelection, stagedClipSelection;

  var <editingMode = false;
  var <drawClipGuides = false;

  var <>didScroll = false;

  var <timelineController;

  selectedClips { ^(clipSelection -- stagedClipSelection) }
  timeSelection_ { |val| timeSelection = val; this.changed(\timeSelection, val); }
  editingMode_ { |val| editingMode = val; this.changed(\editingMode, val); }


  *new { |parent, bounds, timeline, startTime = -2.0, duration = 50.0|
    ^super.new(parent, bounds).init(timeline, startTime, duration);
  }

  init { |argtimeline, argstartTime, argduration|
    var width = this.bounds.width;
    var height = this.bounds.height;

    startTime = argstartTime;
    duration = argduration;

    clipSelection = Set[];
    stagedClipSelection = Set[];

    timeline = argtimeline;
    timelineController = ESTimelineController(timeline, this);

    this.makeTrackViews;
    this.makeContextMenu;

    this.drawFunc = {
      // draw grid
      if (timeline.snapToGrid) {
        var gridDivisionFilter = 1, beatFilter = 1;
        gridDivisionFilter = (this.pixelsToRelativeTime(10) * timeline.gridDivision).asInteger.nextPowerOfTwo;
        if (gridDivisionFilter > timeline.gridDivision) {
          beatFilter = (gridDivisionFilter / timeline.gridDivision).asInteger.nextPowerOfTwo;
        };
        (this.startTime.asInteger.max(0)..(this.startTime + this.duration + 1).asInteger).do { |i|
          if (i % beatFilter == 0) {
            timeline.gridDivision.do { |j|
              if (j % gridDivisionFilter == 0) {
                var left = this.absoluteTimeToPixels(i + (j / timeline.gridDivision));
                Pen.addRect(Rect(left, 0, 1, this.bounds.height));
                Pen.color = if (j == 0) { Color.gray(0, 0.3) } { Color.gray(0, 0.1) };
                Pen.fill;
              };
            };
          };
        };
      };

      if (editingMode) {
        Pen.addRect(Rect(0, 0, this.bounds.width, this.bounds.height));
        Pen.color_(Color.hsv(0.58, 0.45, 0.65, 0.2));
        Pen.fill;
      };
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
    };

    this.mouseWheelAction = { |view, x, y, mods, xDelta, yDelta|
      var xTime = view.pixelsToAbsoluteTime(x);
      if (mods.isCmd) { view.duration = view.duration * (-1 * yDelta).linexp(-100, 100, 0.5, 2, nil); };
      if (mods.isAlt) {
        var ratio = yDelta.linexp(-100, 100, 0.5, 2, nil);
        var diff = y - this.parent.visibleOrigin.y;
        var oldHeight = this.bounds.height;
        this.bounds = this.bounds.height_((oldHeight * ratio).max(this.parent.bounds.height).min(this.parent.bounds.height * timeline.tracks.size * 2));
        ratio = this.bounds.height / oldHeight;
        this.makeTrackViews;
        this.parent.visibleOrigin_(0@((this.parent.visibleOrigin.y + diff) * ratio - diff));
      };
      view.startTime = xTime - view.pixelsToRelativeTime(x);
      view.startTime = view.startTime + (xDelta * view.duration * -0.002);
      dragView.visible_(false);

      if ((timeline.now < startTime) or: (timeline.now > this.endTime)) {
        didScroll = true;
      } {
        didScroll = false;
      };

      if (mods.isCmd or: mods.isAlt) {
        true;
      } {
        false;
      };
    };

    this.mouseDownAction = { |view, x, y, mods, buttNum, clickCount|
      clickPoint = x@y;
      clickTime = this.pixelsToAbsoluteTime(x);
      originalDuration = duration;
      if (hoverClip.notNil) {
        if (this.selectedClips.includes(hoverClip)) {
          hoverClipStartTime = this.selectedClips.asArray.collect(_.startTime);
          hoverClipEndTime = this.selectedClips.asArray.collect(_.endTime);
          hoverClipOffset = this.selectedClips.asArray.collect(_.offset);
        } {
          hoverClipStartTime = hoverClip.startTime;
          hoverClipEndTime = hoverClip.endTime;
          hoverClipOffset = hoverClip.offset;
        };
      } {
        if (clickCount > 1) {
          // double click on empty area to remove time and clip selection
          if (mods.isAlt.not) {
            clipSelection = Set[];
            this.changed(\clipSelection);
          };

          if (mods.isCmd.not) {
            this.timeSelection = nil;
          };
        };
      };

      if ((clickCount > 1) and: hoverClip.notNil) {
        if (mods.isShift) {
          // shift double click to select the clip's time
          this.timeSelection = [hoverClip.startTime, hoverClip.endTime];
        } {
          // edit the clip on double click
          hoverClip.guiClass.new(hoverClip, timeline);
        };
      };

      // alt to duplicate clip
      if (mods.isAlt and: hoverClip.notNil and: editingMode.not) {
        if (this.selectedClips.includes(hoverClip)) {
          duplicatedClips = this.selectedClips.asArray.collect { |clip|
            var duplicatedClip = clip.duplicate;
            if (clip == hoverClip) { hoverClip = duplicatedClip };
            duplicatedClip;
          };
        } {
          duplicatedClips = [hoverClip.duplicate];
        };
      };

      if (hoverClip.notNil and: editingMode) {
        var trackHeights = this.trackHeights;
        var top = 0;
        var i = hoverClip.track.index;
        i.do { |j| top = top + trackHeights[j] };
        // again, not the best:
        hoverClip.drawClip.prMouseDown(x, y - top, mods, buttNum, clickCount, *this.clipBounds(hoverClip))
      };

      if (hoverEnv.notNil) {
        var trackHeights = this.trackHeights;
        var top = 0;
        var i = hoverTrack.index;
        i.do { |j| top = top + trackHeights[j] };
        hoverEnv.prMouseDown(x, y - top, mods, buttNum, clickCount, this.timeSelection);
      };
    };

    this.mouseUpAction = { |view, x, y, mods|
      // if the mouse didn't move during the click, move the playhead to the click point:
      if (clickPoint == (x@y)) {
        if (timeline.isPlaying.not) {
          timeline.now = if (timeline.snapToGrid) { clickTime.round(1 / timeline.gridDivision) } { clickTime };
        };
      };

      if (hoverClip.isNil) {
        clipSelection = this.selectedClips;
        stagedClipSelection = Set[];
        this.changed(\selectedClips);
      };

      if (duplicatedClips.notNil) {
        // this means alt was pressed but mouse did not move
        duplicatedClips.do(_.free);
        duplicatedClips = nil;
      };

      [leftGuideView, rightGuideView].do(_.visible_(false));
      drawClipGuides = false;

      clickPoint = nil;
      clickTime = nil;
      scrolling = false;
      originalDuration = nil;
      hoverClipStartTime = nil;

      timeline.addUndoPoint;
      this.refresh;
      this.changed(\mouseUp);
    };

    this.mouseMoveAction = { |view, x, y, mods|
      var yDelta = y - clickPoint.y;
      var xDelta = x - clickPoint.x;
      var doTimeSelectionOverEnv, doTimeSelectionOverEnvClip;

      if (editingMode.not) {
        switch (hoverCode)
        {1} { // drag left edge
          if (this.selectedClips.includes(hoverClip)) {
            this.selectedClips.do { |clip, i|
              clip.startTime_(hoverClipStartTime[i] + this.pixelsToRelativeTime(xDelta), true);
            };
          } {
            hoverClip.startTime_(this.pixelsToAbsoluteTime(x), true);
          };
          // snap to grid, optionally
          if (timeline.snapToGrid and: (this.relativeTimeToPixels((hoverClip.startTime.round(1 / timeline.gridDivision) - hoverClip.startTime).abs) < 10)) {
            var adjust = hoverClip.startTime.round(1 / timeline.gridDivision) - hoverClip.startTime;
            if (this.selectedClips.includes(hoverClip)) {
              this.selectedClips.do { |clip, i|
                clip.startTime_(clip.startTime + adjust, true);
              };
            } {
              hoverClip.startTime_(hoverClip.startTime + adjust, true);
            };
          };
          // adjust red bar position
          dragView.bounds_(dragView.bounds.left_(this.absoluteTimeToPixels(hoverClip.startTime)));
        }
        {2} { // drag right edge
          if (this.selectedClips.includes(hoverClip)) {
            this.selectedClips.do { |clip, i|
              clip.endTime = hoverClipEndTime[i] + this.pixelsToRelativeTime(xDelta);
            };
          } {
            hoverClip.endTime = this.pixelsToAbsoluteTime(x);
          };
          // snap to grid, optionally
          if (timeline.snapToGrid and: (this.relativeTimeToPixels((hoverClip.endTime.round(1 / timeline.gridDivision) - hoverClip.endTime).abs) < 10)) {
            var adjust = hoverClip.endTime.round(1 / timeline.gridDivision) - hoverClip.endTime;
            if (this.selectedClips.includes(hoverClip)) {
              this.selectedClips.do { |clip, i|
                clip.endTime = clip.endTime + adjust;
              };
            } {
              hoverClip.endTime = hoverClip.endTime + adjust;
            };
          };
          dragView.bounds_(dragView.bounds.left_(this.absoluteTimeToPixels(hoverClip.endTime) - 2));
        }
        {0} { // drag clip
          if (mods.isCmd) {
            hoverClip.offset = hoverClipOffset - this.pixelsToRelativeTime(xDelta);
          } {
            var currentHoverTrack = this.trackAtY(y);

            // if clips have been duplicated, insert them into appropriate tracks and then select and move.
            if (duplicatedClips.notNil) {
              duplicatedClips.do { |duplicatedClip|
                duplicatedClip.track.addClip(duplicatedClip);
              };
              if (duplicatedClips.size > 1) {
                clipSelection = duplicatedClips.asSet;
                hoverClipStartTime = this.selectedClips.asArray.collect(_.startTime);
              };
              duplicatedClips = nil;
            };

            // move clips
            if (this.selectedClips.includes(hoverClip)) {
              this.selectedClips.do { |clip, i|
                clip.startTime = hoverClipStartTime[i] + this.pixelsToRelativeTime(xDelta);
              };
            } {
              hoverClip.startTime = hoverClipStartTime + this.pixelsToRelativeTime(xDelta);
            };
            // always snap to cursor
            if (this.relativeTimeToPixels((hoverClip.startTime - timeline.now).abs) < 10) {
              var adjust = timeline.now - hoverClip.startTime;
              if (this.selectedClips.includes(hoverClip)) {
                this.selectedClips.do { |clip, i|
                  clip.startTime = clip.startTime + adjust;
                };
              } {
                hoverClip.startTime = timeline.now;
              };
            };
            // snap to grid, optionally
            if (timeline.snapToGrid and: (this.relativeTimeToPixels((hoverClip.startTime.round(1 / timeline.gridDivision) - hoverClip.startTime).abs) < 10)) {
              var adjust = hoverClip.startTime.round(1 / timeline.gridDivision) - hoverClip.startTime;
              if (this.selectedClips.includes(hoverClip)) {
                this.selectedClips.do { |clip, i|
                  clip.startTime = clip.startTime + adjust;
                };
              } {
                hoverClip.startTime = hoverClip.startTime + adjust;
              };
            };
            // move clips between tracks
            if (currentHoverTrack.index != hoverTrack.index) {
              var trackDelta = currentHoverTrack.index - hoverTrack.index;
              var clips;
              if (this.selectedClips.includes(hoverClip)) {
                clips = this.selectedClips;
              } {
                clips = [hoverClip]
              };
              clips.do { |clip|
                var oldTrack = clip.track.index;
                var newTrack = (oldTrack + trackDelta).clip(0, timeline.tracks.size - 1);
                clip.track.removeClip(clip.index, false);
                timeline.tracks[newTrack].addClip(clip);
              };
              hoverTrack = currentHoverTrack;
            };
          };
        };

        // draw clip guides
        if (hoverCode.notNil) {
          leftGuideView.bounds_(leftGuideView.bounds.left_(this.absoluteTimeToPixels(hoverClip.startTime)));
          rightGuideView.bounds_(rightGuideView.bounds.left_(this.absoluteTimeToPixels(hoverClip.endTime)));
          [leftGuideView, rightGuideView].do(_.visible_(true));
          drawClipGuides = true;
        } {
          [leftGuideView, rightGuideView].do(_.visible_(false));
          drawClipGuides = false;
        };
      } {  // if editingMode
        if (hoverClip.notNil) {
          var trackHeights = this.trackHeights;
          var top = 0;
          var i = hoverClip.track.index;
          i.do { |j| top = top + trackHeights[j] };
          //                        not the best
          doTimeSelectionOverEnvClip = hoverClip.drawClip.prMouseMove(x, y - top, xDelta, yDelta, mods, *this.clipBounds(hoverClip));
        };
      };// end editingMode

      if (hoverEnv.notNil) {
        var trackHeights = this.trackHeights;
        var top = 0;
        var i = hoverTrack.index;
        i.do { |j| top = top + trackHeights[j] };
        doTimeSelectionOverEnv = hoverEnv.prMouseMove(x, y - top, xDelta, yDelta, mods, this.timeSelection);
      };

      // select time/clips if start dragging from empty area
      if ((hoverClip.isNil or: { doTimeSelectionOverEnvClip ? false }) and: (hoverEnv.isNil or: { doTimeSelectionOverEnv ? false })) {
        if (xDelta.abs > 1) {
          var timeA = this.pixelsToAbsoluteTime(clickPoint.x);
          var timeB = this.pixelsToAbsoluteTime(x);

          if (mods.isAlt.not) {
            if (mods.isShift.not) {
              clipSelection = Set[];
            };
            stagedClipSelection = Set.newFrom(timeline.clipsInRange(this.trackAtY(clickPoint.y).index, this.trackAtY(y).index, timeA, timeB));
            this.changed(\selectedClips);
          } {
            stagedClipSelection = Set[];
          };

          if (mods.isCmd.not) {
            var arr = [timeA, timeB].sort;
            if (timeline.snapToGrid) { arr = arr.round(1 / timeline.gridDivision) };
            this.timeSelection = arr;
          };
        };
      };

      this.changed(\mouseMove);
    };

    this.mouseOverAction = { |view, x, y|
      var i, j;
      var oldHoverClip = hoverClip;
      var oldHoverEnv = hoverEnv;
      var trackHeights = this.trackHeights;
      var top = 0;
      # hoverClip, i, j, hoverCode = this.clipAtPoint(x@y);
      hoverTrack = timeline.tracks[i];
      hoverTime = this.pixelsToAbsoluteTime(x);
      i.do { |j| top = top + trackHeights[j] };

      if (editingMode.not) {
        switch (hoverCode)
        {1} { // left edge
          dragView.bounds_(dragView.bounds.origin_(this.absoluteTimeToPixels(hoverClip.startTime)@top));
          dragView.visible_(true);
        }
        {2} { // right edge
          dragView.bounds_(dragView.bounds.origin_((this.absoluteTimeToPixels(hoverClip.endTime) - 2)@top));
          dragView.visible_(true);
        }
        { // default
          dragView.visible_(false);
        };
      } {
        if (oldHoverClip.notNil and: (oldHoverClip != hoverClip)) {
          oldHoverClip.drawClip.prHoverLeave;
        };
        if (hoverClip.notNil) {
          //                  this is bad:
          hoverClip.drawClip.prHover(x, y - top, hoverTime, *this.clipBounds(hoverClip));
        };
      };

      // pass hover on to envelope view
      hoverEnv = this.envAtY(y);
      if (hoverEnv.notNil) {
        hoverEnv.prHover(x, (y - top), hoverTime)
      };
      if (oldHoverEnv.notNil and: (oldHoverEnv != hoverEnv)) {
        oldHoverEnv.prHoverLeave;
      };
    };

    this.keyDownAction = { |view, char, mods, unicode, keycode, key|
      var snappedHoverTime = if (timeline.snapToGrid) { hoverTime.round(1 / timeline.gridDivision) } { hoverTime };
      var newClipStartTime = if (timeSelection.notNil) { timeSelection[0] } { snappedHoverTime };
      var newClipDuration = if (timeSelection.notNil) { timeSelection[1] - timeSelection[0] } { nil }; // override with default later
      //key.postln;

      hoverTrack = hoverTrack ?? timeline.tracks[0];

      // space is play
      if (char == $ ) { timelineController.togglePlay };
      // enter goes to beginning
      if (key == 16777220) { timelineController.goto(0) };
      // [ and ] move playhead to nearest clip edge to cursor on hovered track
      if ((char == $[) or: (char == $])) {
        timeline.now = hoverTime;
        timelineController.moveToClipEdge(char == $], hoverTrack);
      };
      // opt- left and right move playhead from where it is to nearest clip edge on hovered track
      if (((key == 16777236) or: (key == 16777234)) and: (mods.isAlt)) {
        timelineController.moveToClipEdge(key == 16777236, hoverTrack);
      };
      // s - split clip
      // opt-s = toggle snapping
      if (char == $s) {
        if (mods.isAlt) {
          timelineController.toggleSnap;
        } {
          timelineController.splitClip(hoverClip, snappedHoverTime);
        };
      };
      // cmd-s save cmd-o open cmd-n new
      if ((key == 83) and: (mods.isCmd)) { timelineController.saveAsDialog };
      if ((key == 79) and: (mods.isCmd)) { timelineController.openDialog };
      if ((key == 78) and: (mods.isCmd)) { timelineController.new };
      // m - mute clip
      if (char == $m) { timelineController.toggleMuteClips(hoverClip, this.selectedClips); };
      if (char == $S) { timelineController.newSynthClip(hoverTrack, newClipStartTime, newClipDuration); };
      if (char == $T) { timelineController.newTimelineClip(hoverTrack, newClipStartTime, newClipDuration); };
      if (char == $C) { timelineController.newCommentClip(hoverTrack, newClipStartTime, newClipDuration); };
      if (char == $P) { timelineController.newPatternClip(hoverTrack, newClipStartTime, newClipDuration); };
      if (char == $R) { timelineController.newRoutineClip(hoverTrack, newClipStartTime, newClipDuration); };
      if (char == $E) { timelineController.newEnvClip(hoverTrack, newClipStartTime, newClipDuration); };
      if (char == $e) {
        if (hoverClip.class == ESTimelineClip) {
          ESFuncEditView(hoverClip.timeline);
        } {
          timelineController.editClip(hoverClip);
        };
      };
      // cmd-e toggles editing mode
      if ((key == 69) and: mods.isCmd) {
        this.editingMode = editingMode.not
      };
      // cmd-a selects all
      if ((key == 65) and: mods.isCmd) {
        clipSelection = timeline.clips.asSet;
        this.changed(\selectedClips);
      };
      // cmd-i insert time
      if ((key == 73) and: mods.isCmd) { timelineController.insertTime(timeSelection) };
      // delete - remove clip, cmd - remove track
      if (key == 16777219) {
        if (mods.isCmd) {
          if (mods.isShift) {
            timelineController.deleteTime(timeSelection);
          } {
            if (hoverTrack.notNil and: hoverTrack.index.notNil) {
              var index = hoverTrack.index;
              timeline.removeTrack(index);
              hoverTrack = timeline.tracks[min(index, timeline.tracks.size - 1)];
            };
          };
        } {
          if (hoverClip.notNil and: this.selectedClips.includes(hoverClip).not) {
            hoverTrack.removeClip(hoverClip.index);
            hoverClip = nil;
          } {
            this.selectedClips.do { |clip|
              clip.track.removeClip(clip.index);
            };
          };
        };
      };
      // cmd-t new track (shift before, default after)
      if (mods.isCmd and: (key == 84)) {
        if (hoverTrack.notNil) {
          if (mods.isShift) {
            timeline.addTrack(hoverTrack.index);
          } {
            timeline.addTrack(hoverTrack.index + 1);
          };
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
    };
  }

  makeContextMenu {
    this.setContextMenuActions(
      Menu(
        MenuAction("New Comment (C)", { timelineController.newCommentClip(hoverTrack, hoverTime) }),
        MenuAction("New Synth Clip (S)", { timelineController.newSynthClip(hoverTrack, hoverTime) }),
        MenuAction("New Pattern Clip (P)", { timelineController.newPatternClip(hoverTrack, hoverTime) }),
        MenuAction("New Routine Clip (R)", { timelineController.newRoutineClip(hoverTrack, hoverTime) }),
        MenuAction("New Env Clip (E)", { timelineController.newEnvClip(hoverTrack, hoverTime) }),
        MenuAction("New Timeline Clip (T)", { timelineController.newTimelineClip(hoverTrack, hoverTime) })
      ).title_("New Clip"),
      Menu(
        Menu(
          MenuAction("Set Env range keeping breakpoint values", { timelineController.setEnvRange(hoverClip, this.selectedClips) }),
        ).title_("Env actions"),
        Menu(
          MenuAction("Add Env for Synth argument", { timelineController.addEnvForSynth(hoverClip, this.selectedClips) }),
        ).title_("Synth actions"),
        Menu(
          MenuAction("Bulk edit selected clips", { timelineController.bulkEditSelectedClips(hoverClip, this.selectedClips) }),
          MenuAction.separator(""),
          MenuAction("Bulk edit Synth defName", { timelineController.bulkEditSynthDefName(hoverClip, this.selectedClips) }),
          MenuAction("Bulk edit (change) Synth arguments", { timelineController.bulkEditSynthArgs(hoverClip, this.selectedClips) }),
          MenuAction("Bulk adjust (modify) Synth arguments", { timelineController.bulkAdjustSynthArgs(hoverClip, this.selectedClips) }),
        ).title_("Bulk actions"),
        MenuAction("Edit Clip (e)", { timelineController.editClip(hoverClip) }),
        MenuAction("Split Clip (s)", { if (hoverClip.notNil) { timelineController.splitClip(hoverClip, hoverTime) } }),
        MenuAction("Mute/unmute Clip (m)", { timelineController.toggleMuteClips(hoverClip, this.selectedClips) }),
        MenuAction("Delete Clip (⌫)", { if (hoverClip.notNil) { hoverTrack.removeClip(hoverClip.index) } }),
      ).title_("Clip actions"),
      MenuAction.separator(""),
      Menu(
        MenuAction("Insert Time (Cmd+i)", { timelineController.insertTime(timeSelection) }),
        MenuAction("Delete Time (Shift+Cmd+⌫)", { timelineController.deleteTime(timeSelection) }),
      ).title_("Time actions"),
      MenuAction.separator(""),
      MenuAction("Add Track Before (Cmd+T)", { timeline.addTrack(hoverTrack.index) }),
      MenuAction("Add Track After (Cmd+t)", { timeline.addTrack(hoverTrack.index + 1) }),
      MenuAction("Delete Track (Cmd+⌫)", { timeline.removeTrack(hoverTrack.index) }),
    );
  }

  trackHeights {
    var heightMultipliers = timeline.tracks.collect(_.totalHeightMultiplier);
    var sum = heightMultipliers.sum;
    var height = this.bounds.height;
    trackHeight = height / sum;
    ^heightMultipliers * trackHeight;
  }

  makeTrackViews {
    // call this when number of tracks changes
    if (this.notNil and: { this.bounds.notNil }) { // wtf?
      var width = this.bounds.width;
      var height = this.bounds.height;
      var trackHeights = this.trackHeights;
      var top = 0;
      // necessary for scroll view
      heightRatio = height / this.parent.bounds.height;

      // make track views
      trackViews.do(_.remove);
      trackViews = timeline.tracks.collect { |track, i|
        var tv = ESTrackView(this, Rect(0, top, width, trackHeights[i]), track);
        top = top + trackHeights[i];
        tv;
      };

      // draw guide lines
      [playheadView, dragView, leftGuideView, rightGuideView].do(_.remove);
      playheadView = UserView(this, this.bounds.copy.origin_(0@0))
      .acceptsMouse_(false)
      .drawFunc_({
        var left = this.absoluteTimeToPixels(timeline.soundingNow);
        var top = 0;
        timeline.tracks.do { |track, i|
          var clip = this.clipAtX(track, left)[0];
          if ((clip.class == ESTimelineClip)/* and: { clip.useParentClock.not }*/ and: { timeline.isPlaying }) {
            if (clip.timeline.isPlaying) {
              var leftOffset = this.absoluteTimeToPixels(clip.startTime + startTime - clip.offset);
              // sounding playhead in black
              left = this.absoluteTimeToPixels(clip.timeline.soundingNow) + leftOffset;
              Pen.addRect(Rect(left, top, 2, trackHeight/*trackHeights[i]*/));
              Pen.color = Color.black;
              Pen.fill;

              // "scheduling playhead" in gray
              if (clip.timeline.now < (clip.offset + clip.duration)) {
                Pen.color = Color.gray(0.5, 0.5);
                left = this.absoluteTimeToPixels(clip.timeline.now) + leftOffset;
                Pen.addRect(Rect(left, top, 2, trackHeight/*trackHeights[i]*/));
                Pen.fill;
              };

              // also draw timeline playhead for envelopes
              left = this.absoluteTimeToPixels(timeline.soundingNow);
              Pen.addRect(Rect(left, top + trackHeight, 2, trackHeights[i] - trackHeight));
              Pen.color = Color.black;
              Pen.fill;
              Pen.color = Color.gray(0.5, 0.5);
              left = this.absoluteTimeToPixels(timeline.now);
              Pen.addRect(Rect(left, top + trackHeight, 2, trackHeights[i] - trackHeight));
              Pen.fill;
            };

            if (timeline.now > clip.endTime) {
              // "scheduling playhead" in gray
              Pen.color = Color.gray(0.5, 0.5);
              left = this.absoluteTimeToPixels(timeline.now);
              Pen.addRect(Rect(left, top, 2, trackHeights[i]));
              Pen.fill;
            };
          } {
            // sounding playhead in black
            left = this.absoluteTimeToPixels(timeline.soundingNow);
            Pen.addRect(Rect(left, top, 2, trackHeights[i]));
            Pen.color = Color.black;
            Pen.fill;

            if (timeline.isPlaying) {
              // "scheduling playhead" in gray
              Pen.color = Color.gray(0.5, 0.5);
              left = this.absoluteTimeToPixels(timeline.now);
              Pen.addRect(Rect(left, top, 2, trackHeights[i]));
              Pen.fill;
            };
          };

          top = top + (trackHeights[i] ?? 0); // for some reason this happens sometimes
        };
      });
      dragView = View(this, Rect(0, 0, 2, trackHeight)).visible_(false).background_(Color.red).acceptsMouse_(false);
      leftGuideView = View(this, Rect(0, 0, 1, height)).visible_(false).background_(Color.gray(0.6)).acceptsMouse_(false);
      rightGuideView = View(this, Rect(0, 0, 1, height)).visible_(false).background_(Color.gray(0.6)).acceptsMouse_(false);

      this.changed(\makeTrackViews);
    };
  }

  clipAtX { |track, x, i|
    track.clips.do { |clip, j|
      // if clip within bounds...
      if ((clip.startTime < this.endTime) and: (clip.endTime > this.startTime)) {
        var left = this.absoluteTimeToPixels(clip.startTime);
        var width = this.relativeTimeToPixels(clip.duration);
        // if our point is within the clip's bounds...
        if (x.inRange(left, left + width)) {
          if ((x - left) < 3) { ^[clip, i, j, 1] }; // code for mouse over left edge
          if (((left + width) - x) < 3) { ^[clip, i, j, 2] }; // code for mouse over right edge
          ^[clip, i, j, 0];
        };
      };
    };
    ^[nil, i, nil, nil];
  }

  clipAtPoint { |point|
    var track = this.trackAtY(point.y, true);
    if (track.isInteger) {
      ^[nil, track, nil, nil];
    } {
      ^this.clipAtX(track, point.x, track.index);
    }
  }

  trackAtY { |y, onlyClip = false|
    var trackHeights = this.trackHeights;
    var top = 0;

    timeline.tracks.do { |track, i|
      var bottom = top + trackHeights[i];
      if (y < bottom) {
        if (onlyClip.not) {
          ^track;
        } {
          if (y < (top + trackHeight)) {
            ^track;
          } {
            ^i;
          };
        }
      };
      top = bottom;
    };
    ^timeline.tracks.last;
  }

  envAtY { |y|
    var trackHeights = this.trackHeights;
    var envHeight = timeline.envHeightMultiplier * trackHeight;
    var top = 0;

    timeline.tracks.do { |track, i|
      var bottom = top + trackHeights[i];
      if (y < bottom) {
        if (y > (top + trackHeight)) {
          var envIndex = ((y - (top + trackHeight)) / envHeight).asInteger;
          ^track.envs[envIndex].value;
        } {
          ^nil;
        };
      };
      top = bottom;
    };

    // this should never happen
    ^nil.debug("ESTimelineView envAtY");
  }

  // helper methods:
  relativeTimeToPixels { |time| ^(time / duration) * this.bounds.width }
  absoluteTimeToPixels { |clipStartTime| ^this.relativeTimeToPixels(clipStartTime - startTime) }
  pixelsToRelativeTime { |pixels| ^(pixels / this.bounds.width) * duration }
  pixelsToAbsoluteTime { |pixels| ^this.pixelsToRelativeTime(pixels) + startTime }

  clipBounds { |clip|
    var left = this.absoluteTimeToPixels(clip.startTime);
    var width = this.relativeTimeToPixels(clip.duration);
    ^[left, 3, width, trackHeight - 4, editingMode, nil, nil, this.selectedClips.includes(clip), true, timeSelection.collect({ |time| this.absoluteTimeToPixels(time) }), if (timeSelection.notNil) { timeSelection - clip.startTime } { nil }];
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