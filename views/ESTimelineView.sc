ESTimelineView : UserView {
  var <timeline;
  var <trackViews, <playheadView;
  var <dragView, <leftGuideView, <rightGuideView;
  var <startTime, <duration;
  var <trackHeight, <heightRatio = 1;
  var clickPoint, clickTime, scrolling = false, originalDuration;
  var <hoverClip, <hoverCode, hoverClipStartTime, hoverClipEndTime, hoverClipOffset;
  var hoverTime = 0, hoverTrack = 0;
  var duplicatedClips, <timeSelection, clipSelection, stagedClipSelection;

  var <editingMode = false;
  var <drawClipGuides = false;

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

    this.makeTrackViews;

    this.setContextMenuActions(
      Menu(
        MenuAction("Add Comment (C)", { timeline.tracks[hoverTrack].addClip(ESClip(hoverTime, 5)) }),
        MenuAction("Add Synth Clip (S)", { timeline.tracks[hoverTrack].addClip(ESSynthClip(hoverTime, 0.5, defName: \default)) }),
        MenuAction("Add Pattern Clip (P)", { timeline.tracks[hoverTrack].addClip(ESPatternClip(hoverTime, 5, pattern: {Pbind()})) }),
        MenuAction("Add Routine Clip (R)", { timeline.tracks[hoverTrack].addClip(ESRoutineClip(hoverTime, 5, func: {})) }),
        MenuAction("Add Env Clip (E)", { timeline.tracks[hoverTrack].addClip(ESEnvClip(hoverTime, 5, env: Env([0, 1, 0], [2.5, 2.5], \sin), prep: true)); }),
        MenuAction("Add Timeline Clip (T)", { timeline.tracks[hoverTrack].addClip(ESTimelineClip(hoverTime, 10, timeline: ESTimeline())); })
      ).title_("Add Clip"),
      Menu(
        MenuAction("Add Env for Synth argument", {
          if (hoverClip.class == ESSynthClip) {
            var names = hoverClip.argControls.collect(_.name);
            var thisHoverClip = hoverClip;
            var thisTrackIndex = thisHoverClip.track.index + 1;
            ESBulkEditWindow.menu("Add Env for Synth argument",
              "arg", names, names.indexOf(\amp),
              "add track", true,
              callback: { |argument, addTrack|
                var envClip;
                var name = argument.asSymbol;
                var value = thisHoverClip.getArg(name).value;
                var i = 0, envName, min = 0, max = 1, isExponential = false;
                if (value.isNumber.not) { value = 0 };
                min = min(min, value);
                max = max(max, value);
                if (name.asSpec.notNil) {
                  var spec = name.asSpec;
                  min = spec.minval;
                  max = spec.maxval;
                  isExponential = (spec.warp.class == ExponentialWarp);
                };
                while { timeline[envName = (name ++ i).asSymbol].notNil } { i = i + 1 };
                if (addTrack) {
                  timeline.addTrack(thisTrackIndex);
                };
                envClip = ESEnvClip(
                  thisHoverClip.startTime, thisHoverClip.duration,
                  name: envName,
                  target: thisHoverClip.target,
                  min: min,
                  max: max,
                  isExponential: isExponential
                ); // dont prep here anymore because it needs to know its track
                envClip.env = Env(envClip.prValueUnscale(value).dup(2), [thisHoverClip.duration], [0]);
                timeline.tracks[thisTrackIndex].addClip(envClip);
                envClip.prep;

                thisHoverClip.setArg(name, envName);
              }
            );
          };
        }),
        MenuAction("Set Env range keeping breakpoint values", {
          var minDefault = 0, maxDefault = 1, curveDefault = 0, isExponentialDefault = false;
          var arr = if (this.selectedClips.includes(hoverClip)) { this.selectedClips } { [hoverClip] };
          if (hoverClip.class == ESEnvClip) {
            minDefault = hoverClip.min;
            maxDefault = hoverClip.max;
            curveDefault = hoverClip.curve;
            isExponentialDefault = hoverClip.isExponential;
          };
          ESBulkEditWindow.keyValue("Set Env range keeping breakpoint values",
            "min", minDefault, "max", maxDefault, "isExponential", isExponentialDefault, true, "curve", curveDefault,
            callback: { |min, max, isExponential, curve|
              min = min.interpret;
              max = max.interpret;
              curve = curve.interpret;
              if ((isExponential and: ((min.sign != max.sign))).not) {
                arr.do { |clip|
                  if (clip.class == ESEnvClip) {
                    var oldLevels = clip.env.levels;
                    var values = oldLevels.collect(clip.prValueScale(_));
                    var newLevels;
                    clip.min = min;
                    clip.max = max;
                    clip.curve = curve;
                    clip.isExponential = isExponential;
                    newLevels = values.collect(clip.prValueUnscale(_));
                    clip.env = Env(newLevels, clip.env.times, clip.env.curves);
                  };
                };
              };
            }
          );
        }),
        MenuAction("Bulk edit Synth arguments", {
          var arr = this.selectedClips;
          if (arr.size == 0) { arr = [hoverClip] };
          ESBulkEditWindow.keyValue(callback: { |key, val, hardCode|
            var func;
            key = key.asSymbol;
            val = ("{" ++ val ++ "}").interpret;
            func = { |clips|
              clips.do { |clip|
                if (clip.class == ESSynthClip) {
                  clip.setArg(key, if (hardCode) { val.value } { val });
                };
                if (clip.class == ESTimelineClip) {
                  func.(clip.timeline.clips);
                };
              };
            };
            func.(arr);
          });
        }),
        MenuAction("Bulk edit Synth defName", {
          var arr = this.selectedClips;
          if (arr.size == 0) { arr = [hoverClip] };
          ESBulkEditWindow.value(callback: { |val|
            var func = { |clips|
              clips.do { |clip|
                if (clip.class == ESSynthClip) {
                  clip.defName = val;
                };
                if (clip.class == ESTimelineClip) {
                  func.(clip.timeline.clips);
                };
              };
            };
            func.(arr);
          });
        }),
        MenuAction.separator(""),
        MenuAction("Edit Clip (e)", { hoverClip.guiClass.new(hoverClip, timeline) }),
        MenuAction("Split Clip (s)", { if (hoverClip.notNil) { timeline.tracks[hoverTrack].splitClip(hoverClip.index, hoverTime) } }),
        MenuAction("Delete Clip (⌫)", { if (hoverClip.notNil) { timeline.tracks[hoverTrack].removeClip(hoverClip.index) } }),
      ).title_("Clip actions"),
      MenuAction.separator(""),
      Menu(
        MenuAction("Insert Time (Cmd+i)", { if (timeSelection.notNil) { timeline.insertTime(*timeSelection); }; }),
        MenuAction("Delete Time (Shift+Cmd+⌫)", { if (timeSelection.notNil) { timeline.deleteTime(*timeSelection); }; }),
      ).title_("Time actions"),
      MenuAction.separator(""),
      MenuAction("Add Track Before (Cmd+T)", { timeline.addTrack(hoverTrack) }),
      MenuAction("Add Track After (Cmd+t)", { timeline.addTrack(hoverTrack + 1) }),
      MenuAction("Delete Track (Cmd+⌫)", { timeline.removeTrack(hoverTrack) }),
    );

    this.drawFunc_({
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
      if (mods.isAlt and: hoverClip.notNil) {
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
        // again, not the best:
        hoverClip.prMouseDown(x, y - (trackHeight * hoverClip.track.index), mods, buttNum, clickCount, *this.clipBounds(hoverClip))
      };
    }).mouseUpAction_({ |view, x, y, mods|
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
    }).mouseMoveAction_({ |view, x, y, mods|
      var yDelta = y - clickPoint.y;
      var xDelta = x - clickPoint.x;

      // select time/clips if start dragging from empty area
      if (hoverClip.isNil) {
        if (xDelta.abs > 1) {
          var timeA = this.pixelsToAbsoluteTime(clickPoint.x);
          var timeB = this.pixelsToAbsoluteTime(x);

          if (mods.isAlt.not) {
            if (mods.isShift.not) {
              clipSelection = Set[];
            };
            stagedClipSelection = Set.newFrom(timeline.clipsInRange(this.trackAtY(clickPoint.y), this.trackAtY(y), timeA, timeB));
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
            if (currentHoverTrack != hoverTrack) {
              var trackDelta = currentHoverTrack - hoverTrack;
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
          //                        not the best
          hoverClip.prMouseMove(x, y - (trackHeight * hoverClip.track.index), xDelta, yDelta, mods, *this.clipBounds(hoverClip));
        };
      };// end editingMode

      this.changed(\mouseMove);
    }).mouseOverAction_({ |view, x, y|
      var i, j;
      var oldHoverClip = hoverClip;
      # hoverClip, i, j, hoverCode = this.clipAtPoint(x@y);
      hoverTrack = i;
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
      var snappedHoverTime = if (timeline.snapToGrid) { hoverTime.round(1 / timeline.gridDivision) } { hoverTime };
      var newClipStartTime = if (timeSelection.notNil) { timeSelection[0] } { snappedHoverTime };
      var newClipDuration = if (timeSelection.notNil) { timeSelection[1] - timeSelection[0] } { nil }; // override with default later
      //key.postln;
      // space is play
      if (char == $ ) { timeline.togglePlay };
      // enter goes to beginning
      if (key == 16777220) { timeline.goto(0) };
      // [ and ] move playhead to nearest clip edge on hovered track
      if ((char == $[) or: (char == $])) {
        var points = [];
        var prevPrevPoint = 0, prevPoint = 0, skipRest = false;
        var goForwards = (char == $]);
        timeline.tracks[hoverTrack].clips.do { |clip|
          points = points ++ [clip.startTime, clip.endTime];
        };
        points.sort.do { |point|
          if (skipRest.not) {
            if ((point > timeline.now) or: (point == points.last)) {
              skipRest = true;
              timeline.now = if (goForwards) { point } {
                if (timeline.now == prevPoint) {
                  prevPrevPoint
                } {
                  prevPoint
                };
              };
            };
          };
          prevPrevPoint = prevPoint;
          prevPoint = point;
        }
      };
      // s - split clip
      // opt-s = toggle snapping
      if (char == $s) {
        if (mods.isAlt) {
          timeline.snapToGrid = timeline.snapToGrid.not;
        } {
          if (hoverClip.notNil) { timeline.tracks[hoverTrack].splitClip(hoverClip.index, snappedHoverTime) };
        };
      };
      // m - mute clip
      if (char == $m) {
        if (hoverClip.notNil and: this.selectedClips.includes(hoverClip).not) {
          hoverClip.mute = hoverClip.mute.not
        } {
          this.selectedClips.do { |clip|
            clip.mute = clip.mute.not
          };
        };
      };
      if (char == $S) {
        timeline.tracks[hoverTrack].addClip(ESSynthClip(newClipStartTime, newClipDuration ?? 0.5, defName: \default));
      };
      if (char == $T) {
        timeline.tracks[hoverTrack].addClip(ESTimelineClip(newClipStartTime, newClipDuration ?? 10, timeline: ESTimeline()));
      };
      if (char == $C) {
        timeline.tracks[hoverTrack].addClip(ESClip(newClipStartTime, newClipDuration ?? 5));
      };
      if (char == $P) {
        timeline.tracks[hoverTrack].addClip(ESPatternClip(newClipStartTime, newClipDuration ?? 5, pattern: {Pbind()}));
      };
      if (char == $R) {
        timeline.tracks[hoverTrack].addClip(ESRoutineClip(newClipStartTime, newClipDuration ?? 5, func: {}));
      };
      if (char == $E) {
        var thisDuration = newClipDuration ?? 5;
        timeline.tracks[hoverTrack].addClip(ESEnvClip(newClipStartTime, thisDuration, env: Env([0.5, 0.5], [thisDuration], 0), prep: true));
      };
      if (char == $e) {
        if (hoverClip.class == ESTimelineClip) {
          ESFuncEditView(hoverClip.timeline);
        } {
          hoverClip.guiClass.new(hoverClip, timeline);
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
      if ((key == 73) and: mods.isCmd) {
        if (timeSelection.notNil) {
          timeline.insertTime(*timeSelection);
        };
      };
      // delete - remove clip, cmd - remove track
      if (key == 16777219) {
        if (mods.isCmd) {
          if (mods.isShift) {
            if (timeSelection.notNil) {
              timeline.deleteTime(*timeSelection);
            };
          } {
            timeline.removeTrack(hoverTrack);
          };
        } {
          if (hoverClip.notNil and: this.selectedClips.includes(hoverClip).not) {
            timeline.tracks[hoverTrack].removeClip(hoverClip.index);
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
  }

  makeTrackViews {
    // call this when number of tracks changes
    var width = this.bounds.width;
    var height = this.bounds.height;

    heightRatio = height / this.parent.bounds.height;

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
      var left = this.absoluteTimeToPixels(timeline.soundingNow);
      Pen.use {
        timeline.tracks.do { |track, i|
          var clip = this.clipAtX(track, left)[0];
          if ((clip.class == ESTimelineClip) and: { clip.useParentClock.not } and: { timeline.isPlaying }) {

          } {
            // sounding playhead in black
            left = this.absoluteTimeToPixels(timeline.soundingNow);
            Pen.addRect(Rect(left, i * trackHeight, 2, trackHeight));
            Pen.color = Color.black;
            Pen.fill;

            if (timeline.isPlaying) {
              // "scheduling playhead" in gray
              Pen.color = Color.gray(0.5, 0.5);
              left = this.absoluteTimeToPixels(timeline.now);
              Pen.addRect(Rect(left, i * trackHeight, 2, trackHeight));
              Pen.fill;
            };
          };
        };
      };
    });
    dragView = View(this, Rect(0, 0, 2, trackHeight)).visible_(false).background_(Color.red).acceptsMouse_(false);
    leftGuideView = View(this, Rect(0, 0, 1, height)).visible_(false).background_(Color.gray(0.6)).acceptsMouse_(false);
    rightGuideView = View(this, Rect(0, 0, 1, height)).visible_(false).background_(Color.gray(0.6)).acceptsMouse_(false);

    this.changed(\makeTrackViews);
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
    timeline.tracks.do { |track, i|
      var top = i * trackHeight;
      if (point.y.inRange(top, top + trackHeight)) {
        ^this.clipAtX(track, point.x, i);
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
    ^[left, 3, width, trackHeight - 4, editingMode, nil, nil, this.selectedClips.includes(clip)];
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