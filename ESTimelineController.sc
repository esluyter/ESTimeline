ESTimelineController {
  var <timeline, <timelineView;
  var <lastPath;

  *new { |timeline, timelineView|
    ^super.newCopyArgs(timeline, timelineView);
  }

  togglePlay { timeline.togglePlay }
  goto { |val| timeline.goto(val) }
  toggleSnap { timeline.snapToGrid = timeline.snapToGrid.not }

  splitClip { |clip, time|
    if (clip.notNil) { clip.track.splitClip(clip.index, time) };
  }

  toggleMuteClips { |thisClip, selectedClips|
    // if selectedC does not include thisClip, just mute thisClip
    if (thisClip.notNil and: selectedClips.includes(thisClip).not) {
      thisClip.mute = thisClip.mute.not
    } {
      selectedClips.do { |clip|
        clip.mute = clip.mute.not
      };
    };
  }

  newSynthClip { |track, startTime, duration|
    track.addClip(ESSynthClip(startTime, duration ?? 0.5, defName: \default));
  }

  newTimelineClip { |track, startTime, duration|
    track.addClip(ESTimelineClip(startTime, duration ?? 10, timeline: ESTimeline.newInitMixerChannels(bootOnPrep: timeline.bootOnPrep, useEnvir: timeline.useEnvir, optimizeView: timeline.optimizeView, gridDivision: timeline.gridDivision, snapToGrid: timeline.snapToGrid, useMixerChannel: timeline.useMixerChannel, globalMixerChannelNames: [])));
  }

  newCommentClip { |track, startTime, duration|
    track.addClip(ESClip(startTime, duration ?? 5));
  }

  newPatternClip { |track, startTime, duration|
    track.addClip(ESPatternClip(startTime, duration ?? 5, pattern: {Pbind()}));
  }

  newRoutineClip { |track, startTime, duration|
    track.addClip(ESRoutineClip(startTime, duration ?? 5, func: {}));
  }

  newEnvClip { |track, startTime, duration|
    var thisDuration = duration ?? 5;
    track.addClip(ESEnvClip(startTime, thisDuration, env: Env([0.5, 0.5], [thisDuration], 0), prep: true));
  }

  editClip { |clip|
    clip.guiClass.new(clip, timeline);
  }

  addEnvForSynth { |clip, selectedClips|
    if (clip.class == ESSynthClip) {
      var names = clip.argControls.collect(_.name);
      var thisTrackIndex = clip.track.index;
      var arr = if (selectedClips.includes(clip)) { selectedClips.asArray } { [clip] };
      ESBulkEditWindow.menu("Add Env for Synth argument",
        "arg", names, names.indexOf(\amp),
        "add track", true,
        callback: { |argument, addTrack|
          var envClip;
          var name = argument.asSymbol;
          var i = 0, envName, min = 0, max = 1, isExponential = false;
          if (name.asSpec.notNil) {
            var spec = name.asSpec;
            min = spec.minval;
            max = spec.maxval;
            isExponential = (spec.warp.class == ExponentialWarp);
          };
          if (addTrack) {
            timeline.addTrack(thisTrackIndex, ESTrack([], false, ((clip.track.name ?? "") ++ " envelope").asSymbol, false));
          };

          arr.sort({ |a, b| a.startTime < b.startTime }).do { |thisClip|
            var value = thisClip.getArg(name).value;
            if (value.isNumber.not) { value = 0 };

            while { timeline[envName = (name ++ i).asSymbol].notNil } { i = i + 1 };

            envClip = ESEnvClip(
              thisClip.startTime, thisClip.duration,
              name: envName,
              target: thisClip.target,
              min: min(min, value),
              max: max(max, value),
              isExponential: isExponential
            ); // dont prep here anymore because it needs to know its track
            envClip.env = Env(envClip.prValueUnscale(value).dup(2), [thisClip.duration], [0]);
            timeline.tracks[thisTrackIndex].addClip(envClip);
            envClip.prep;

            thisClip.setArg(name, envName);
          };
        }
      );
    };
  }

  setEnvRange { |clip, selectedClips|
    var minDefault = 0, maxDefault = 1, curveDefault = 0, isExponentialDefault = false;
    var arr = if (selectedClips.includes(clip)) { selectedClips } { [clip] };
    if (clip.class == ESEnvClip) {
      minDefault = clip.min;
      maxDefault = clip.max;
      curveDefault = clip.curve;
      isExponentialDefault = clip.isExponential;
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
  }

  bulkEditSelectedClips { |clip, selectedClips|
    ESBulkEditWindow.code("Bulk edit selected clips:", if (clip.notNil) { "{ |clip|\n  if (clip.class == " ++ clip.class ++ ") {\n  }\n}" } { "{ |clip|\n  \n}" }, callback: { |string|
      var func = string.interpret;
      selectedClips.do(func);
    });
  }

  bulkAdjustSynthArgs { |clip, selectedClips|
    var arr = selectedClips;
    if (arr.size == 0) { arr = [clip] };
    ESBulkEditWindow.keyValue("Bulk adjust Synth args", valDefault: "+ 1", callback: { |key, val, hardCode|
      var func;
      key = key.asSymbol;
      /*val = if (hardCode) {
        ("{ |i| " ++ val ++ "}").interpret;
      } {
        ("{" ++ val ++ "}").interpret;
      };*/
      func = { |clips|
        clips.do { |clip|
          if (clip.class == ESSynthClip) {
            clip.setArg(key, ((if (hardCode) { clip.getArg(key).value } { clip.getArg(key) }).asCompileString ++ val).interpret);
          };
          if (clip.class == ESTimelineClip) {
            func.(clip.timeline.clips);
          };
        };
      };
      func.(arr);
    });
  }

  bulkEditSynthArgs { |clip, selectedClips|
    var arr = selectedClips;
    if (arr.size == 0) { arr = [clip] };
    ESBulkEditWindow.keyValue(callback: { |key, val, hardCode|
      var func;
      var i = 0;
      key = key.asSymbol;
      val = if (hardCode) {
        ("{ |i| " ++ val ++ "}").interpret;
      } {
        ("{" ++ val ++ "}").interpret;
      };
      func = { |clips|
        clips.asArray.sort({ |a, b| a.startTime < b.startTime }).do { |clip|
          if (clip.class == ESSynthClip) {
            clip.setArg(key, if (hardCode) { val.value(i) } { val });
            i = i + 1;
          };
          if (clip.class == ESTimelineClip) {
            func.(clip.timeline.clips);
          };
        };
      };
      func.(arr);
    });
  }

  bulkEditSynthDefName { |clip, selectedClips|
    var arr = selectedClips;
    if (arr.size == 0) { arr = [clip] };
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
  }

  moveToClipEdge { |goForwards, track|
    var points = [];
    var prevPrevPoint = 0, prevPoint = 0, skipRest = false;
    track.clips.do { |clip|
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
  }

  insertTime { |timeSelection|
    if (timeSelection.notNil) { timeline.insertTime(*timeSelection); };
  }

  deleteTime { |timeSelection|
    if (timeSelection.notNil) { timeline.deleteTime(*timeSelection); };
  }

  write { |path|
    File.use(path, "w", { |f| f.write(timeline.currentState.asCompileString) });
    //Document.new("Timeline Score", timeline.currentState.asCompileString).front;
  }

  saveBackup {
    if (lastPath.notNil) {
      this.write(lastPath ++ "-backup.txt");
    }
  }

  saveAsDialog {
    Dialog.savePanel({ |path|
      this.write(path);
      lastPath = path;
    }, path: lastPath);
  }

  openDialog {
    Dialog.openPanel({ |path|
      var func = { |obj, what|
        if (what == \restoreUndoPoint) {
          timelineView.startTime = -2;
          timelineView.duration = timeline.duration.postln + 5;
          timeline.removeDependant(func);
        };
      };

      timeline.restoreUndoPoint(File.readAllString(path).interpret);
      //timeline.restoreUndoPoint(Document.current.string.interpret);
      timeline.addDependant(func);
      lastPath = path;
    }, path: lastPath);
  }

  new {
    lastPath = nil;
    timeline.new;
  }
}