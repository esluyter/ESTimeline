ESTrackPanelView : UserView {
  var timelineView, timeline;
  var trackViews, trackButts;

  *new { |parent, bounds, timelineView|
    ^super.new(parent, bounds).init(timelineView);
  }

  init { |argtimelineView|
    timelineView = argtimelineView;
    timeline = timelineView.timeline;
    this.mouseOverAction = { |view, x, y|
      timelineView.hoverTrack = timelineView.trackAtY(y);
      timelineView.hoverTrack.index.postln;
    };
    this.keyDownAction = { |view, char, mods, unicode, keycode, key|
      // cmd-delete - remove track
      if ((key == 16777219) and: mods.isCmd) {
        if (timelineView.hoverTrack.notNil and: timelineView.hoverTrack.index.notNil) {
          var index = timelineView.hoverTrack.index;
          timeline.removeTrack(index);
          timelineView.hoverTrack = timeline.tracks[min(index, timeline.tracks.size - 1)];
        };
      };
      // cmd-t new track (shift before, default after)
      if (mods.isCmd and: (key == 84)) {
        if (timelineView.hoverTrack.notNil) {
          if (mods.isShift) {
            timeline.addTrack(timelineView.hoverTrack.index);
          } {
            timeline.addTrack(timelineView.hoverTrack.index + 1);
          };
        };
      };
    };
    this.makeTrackViews;
  }

  refresh {
    timeline.tracks.do { |track, i|
      trackButts[i][\mute].value = track.mute.asInteger;
      trackButts[i][\solo].value = track.solo.asInteger;
      trackButts[i][\nameText].string = track.name ?? "";
      trackButts[i][\mix].value_(track.useMixerChannel).visible_(track.timeline.useMixerChannel);
    };
    ^super.refresh;
  }

  makeTrackViews {
    // call this when number of tracks changes
    var width = this.bounds.width;
    var height = timelineView.bounds.height;
    var trackHeights = timelineView.trackHeights;
    var top = 0;

    this.bounds_(this.bounds.height_(height));

    trackButts = [];
    trackViews.do(_.remove);

    trackViews = timeline.tracks.collect { |track, i|
      var ev;
      var view = UserView(this, Rect(0, top, width, trackHeights[i]))
      .drawFunc_({
        Pen.addRect(Rect(0, 0, width, height));
        Pen.color = Color.gray(if (track.shouldPlay) { 0.8 } { 0.68 });
        Pen.fill;
        Pen.addRect(Rect(0, 0, width, 1));
        Pen.color = Color.gray(0.55);
        Pen.fill;
        Pen.addRect(Rect(width - 1, 0, 1, trackHeights[i]));
        Pen.color = Color.gray(0.65);
        Pen.fill;
        if (track.name.notNil) {
          Pen.color = Color.gray(1, 0.3);
          Pen.addRect(Rect(0, 33, width, timelineView.trackHeight - 33));
          Pen.fill;
        };
        track.envs.do { |val, i|
          var envHeight = timelineView.trackHeight * timeline.envHeightMultiplier;
          //[key, val, i, envHeight].postln;
          Pen.addRect(Rect(0, timelineView.trackHeight + (envHeight * i), this.bounds.width, 1));
          Pen.color = Color.gray(0.7);
          Pen.fill;
        };
      })
      .mouseMoveAction_({
        view.beginDrag;
      })
      .mouseOverAction_(false)
      .beginDragAction_({
        track;
      })
      .canReceiveDragHandler_(true)
      .receiveDragHandler_({
        var thisTrack = View.currentDrag;
        timeline.removeTrack(thisTrack.index, false);
        timeline.addTrack(i, thisTrack);
      });
      StaticText(view, Rect(2, 4, 18, 25)).string_(i.asString).stringColor_(Color.gray(0.4)).font_(Font.monospace(14));

      ev = (
        nameText: StaticText(view, Rect(2, 35, width - 4, timelineView.trackHeight - 35)).align_(\topLeft).string_(track.name ?? "").stringColor_(Color.gray(0.5)).font_(Font.sansSerif(14, true)).canReceiveDragHandler_(true)
        .receiveDragHandler_({
          var thisTrack = View.currentDrag;
          timeline.moveTrack(thisTrack.index, i);
        }).mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
          if (clickCount > 1) {
            var nextName = track.name ?? "";
            ev[\nameField].string_(nextName).select(nextName.asString.size, 0).visible_(true).focus;
          };
        })
        .mouseOverAction_(false),
        nameField: TextView(view, Rect(2, 35, width - 4, timelineView.trackHeight - 40)).keyDownAction_({ |...args| this.handleKey(track, *args) }).focusLostAction_({ |view|
          // if you click somewhere else, accept changes
          //try { // why?
            if (view.visible) {
              track.name = if (view.string == "") { nil } { view.string.asSymbol };
            };
            view.visible = false;
          //};
        }).mouseOverAction_(false).visible_(false).font_(Font.sansSerif(14)),
        mix: Button(view, Rect(21, 4, 25, 25)).states_([
          ["mix", Color.gray(0.5), Color.gray(0.8)],
          ["mix", Color.gray(0.85), Color.gray(0.45)]])
        .focusColor_(Color.clear).font_(Font.sansSerif(12, true)).action_({ |view|
          track.useMixerChannel = view.value.asBoolean;
          timelineView.focus;
        }).value_(track.useMixerChannel).visible_(track.timeline.useMixerChannel),
        mute: Button(view, Rect(48, 4, 25, 25)).states_([
          ["M", Color.gray(0.5), Color.gray(0.8)],
          ["M", Color.gray(0.7), Color.gray(0.3)]])
        .focusColor_(Color.clear).font_(Font.sansSerif(16, true)).action_({ |view|
          track.mute = view.value.asBoolean;
          timelineView.focus;
        }).value_(track.mute),
        solo: Button(view, Rect(75, 4, 25, 25)).states_([
          ["S", Color.gray(0.5), Color.gray(0.8)],
          ["S", Color.yellow, Color.black]])
        .focusColor_(Color.clear).font_(Font.sansSerif(16, true)).action_({ |view|
          track.solo = view.value.asBoolean;
          timelineView.focus;
        }).value_(track.solo),
      );
      track.envs.do { |assoc, i|
        var envHeight = timelineView.trackHeight * timeline.envHeightMultiplier;
        var key = assoc.key;
        var env = assoc.value;
        //[key, val, i, envHeight].postln;
        StaticText(view, Rect(5, timelineView.trackHeight + (envHeight * i) + 4, width - 10, envHeight)).string_(key).align_(\topRight).stringColor_(Color.gray(0.6)).setContextMenuActions(
          MenuAction("Set automation range", {
            ESBulkEditWindow.keyValue("Set automation range keeping breakpoint values",
              "min", env.min, "max", env.max, "isExponential", env.isExponential, true, "curve", env.curve,
              callback: { |min, max, isExponential, curve|
                min = min.interpret;
                max = max.interpret;
                curve = curve.interpret;
                if ((isExponential and: ((min.sign != max.sign))).not) {
                  var oldLevels = env.env.levels;
                  var values = oldLevels.collect(env.prValueScale(_));
                  var newLevels;
                  env.min = min;
                  env.max = max;
                  env.curve = curve;
                  env.isExponential = isExponential;
                  newLevels = values.collect(env.prValueUnscale(_));
                  env.env = Env(newLevels, env.env.times, env.env.curves);
                };
              }
            );
          }),
          MenuAction("Remove automation envelope", {
            var template = track.mixerChannelTemplate;
            // hacky
            switch (key.asSymbol)
            { \level } {
              var thisLevel = template.envs.level.valueAtTime(timeline.soundingNow);
              template.envs.level = nil;
              template.level = thisLevel;
              track.mixerChannel.level = thisLevel;
            }
            { \pan } {
              var thisLevel = template.envs.pan.valueAtTime(timeline.soundingNow);
              template.envs.pan = nil;
              template.pan = thisLevel;
              track.mixerChannel.pan = thisLevel;
            };

            if (key.asString.beginsWith("post")) {
              var index = key.asString.split($_)[1].interpret;
              var arr = template.envs.postSends;
              var val = arr[index].valueAtTime(timeline.soundingNow);
              arr[index].stop; arr[index] = nil;
              template.envs.postSends = arr;
              template.postSends[index][1] = val;
              track.mixerChannel.postSends[index].level = val;
            };
            if (key.asString.beginsWith("pre")) {
              var index = key.asString.split($_)[1].interpret;
              var arr = template.envs.preSends;
              var val = arr[index].valueAtTime(timeline.soundingNow);
              arr[index].stop; arr[index] = nil;
              template.envs.preSends = arr;
              template.preSends[index][1] = val;
              track.mixerChannel.preSends[index].level = val;
            };
            if (key.asString.beginsWith("fx")) {
              var index = key.asString.split($_)[1].interpret;
              var param = key.asString.split($_)[2].asSymbol;
              var arr = template.envs.fx;
              arr[index][param].stop; arr[index][param] = nil;
              template.envs.fx = arr;
            };
          })
        );
      };
      trackButts = trackButts.add(ev);
      top = top + trackHeights[i];
      view;
    };
  }

  handleKey { |track, view, char, mods, unicode, keycode, key|
    var nextName;
    //key.postln;
    // enter to accept
    if (key == 16777220) {
      track.name = if (view.string == "") { nil } { view.string.asSymbol };
      view.visible = false;
      timeline.addUndoPoint;
      ^true;
    };
    // tab to accept and move to next
    if (key == 16777217) {
      track.name = if (view.string == "") { nil } { view.string.asSymbol };
      view.visible = false;
      nextName = track.timeline.tracks[track.index + 1].name ?? "";
      trackButts[track.index + 1].nameField.string_(nextName).visible_(true).select(nextName.asString.size, 0).focus;
      timeline.addUndoPoint;
      ^true;
    };
    // shift tab move to previous
    if (key == 16777218) {
      track.name = if (view.string == "") { nil } { view.string.asSymbol };
      view.visible = false;
      nextName = track.timeline.tracks[track.index - 1].name ?? "";
      trackButts[track.index - 1].nameField.string_(nextName).select(nextName.asString.size, 0).visible_(true).focus;
      timeline.addUndoPoint;
      ^true;
    };
    // esc to cancel
    if (key == 16777216) {
      view.visible = false;
      view.string = (track.name ?? "");
      ^true;
    };
  }
}