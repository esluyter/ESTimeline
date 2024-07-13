ESTrackPanelView : UserView {
  var timelineView, timeline;
  var trackViews, trackButts;

  *new { |parent, bounds, timelineView|
    ^super.new(parent, bounds).init(timelineView);
  }

  init { |argtimelineView|
    timelineView = argtimelineView;
    timeline = timelineView.timeline;
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
    var trackHeight = height / timeline.tracks.size;

    this.bounds_(this.bounds.height_(height));

    trackButts = [];
    trackViews.do(_.remove);

    trackViews = timeline.tracks.collect { |track, i|
      var ev;
      var top = i * trackHeight;
      var view = UserView(this, Rect(0, top, width, trackHeight))
      .drawFunc_({
        Pen.addRect(Rect(0, 0, width, height));
        Pen.color = Color.gray(if (track.shouldPlay) { 0.8 } { 0.68 });
        Pen.fill;
        Pen.addRect(Rect(0, 0, width, 1));
        Pen.color = Color.gray(0.55);
        Pen.fill;
        Pen.addRect(Rect(width - 1, 0, 1, trackHeight));
        Pen.color = Color.gray(0.65);
        Pen.fill;
        if (track.name.notNil) {
          Pen.color = Color.gray(1, 0.3);
          Pen.addRect(Rect(0, 33, width, trackHeight - 36));
          Pen.fill;
        };
      })
      .mouseMoveAction_({
        view.beginDrag;
      })
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
        nameText: StaticText(view, Rect(2, 35, width - 4, trackHeight - 35)).align_(\topLeft).string_(track.name ?? "").stringColor_(Color.gray(0.5)).font_(Font.sansSerif(14, true)).canReceiveDragHandler_(true)
        .receiveDragHandler_({
          var thisTrack = View.currentDrag;
          timeline.removeTrack(thisTrack.index, false);
          timeline.addTrack(i, thisTrack);
        }).mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
          if (clickCount > 1) {
            var nextName = track.name ?? "";
            ev[\nameField].string_(nextName).select(nextName.asString.size, 0).visible_(true).focus;
          };
        }),
        nameField: TextView(view, Rect(2, 35, width - 4, trackHeight - 40)).keyDownAction_({ |...args| this.handleKey(track, *args) }).focusLostAction_({ |view|
          // if you click somewhere else, accept changes
          if (view.visible) {
            track.name = if (view.string == "") { nil } { view.string.asSymbol };
          };
          view.visible = false;
        }).visible_(false).font_(Font.sansSerif(14)),
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
      trackButts = trackButts.add(ev);
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
      ^true;
    };
    // tab to accept and move to next
    if (key == 16777217) {
      track.name = if (view.string == "") { nil } { view.string.asSymbol };
      view.visible = false;
      nextName = track.timeline.tracks[track.index + 1].name ?? "";
      trackButts[track.index + 1].nameField.string_(nextName).visible_(true).select(nextName.asString.size, 0).focus;
      ^true;
    };
    // shift tab move to previous
    if (key == 16777218) {
      track.name = if (view.string == "") { nil } { view.string.asSymbol };
      view.visible = false;
      nextName = track.timeline.tracks[track.index - 1].name ?? "";
      trackButts[track.index - 1].nameField.string_(nextName).select(nextName.asString.size, 0).visible_(true).focus;
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