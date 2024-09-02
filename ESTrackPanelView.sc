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
      //trackViews[i].background = Color.gray(if (track.shouldPlay) { 0.75 }, { 0.6 });
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
      var top = i * trackHeight;
      var view = UserView(this, Rect(0, top, width, trackHeight))/*.background_(Color.gray(0.75))*/
      .drawFunc_({
        Pen.addRect(Rect(0, 0, width, height));
        Pen.color = Color.gray(if (track.shouldPlay) { 0.78 } { 0.68 });
        Pen.fill;
        Pen.addRect(Rect(0, 0, width, 1));
        Pen.color = Color.gray(0.55);
        Pen.fill;
        Pen.addRect(Rect(width - 1, 0, 1, trackHeight));
        Pen.color = Color.gray(0.65);
        Pen.fill;
      });
      StaticText(view, Rect(2, 2, 18, 20)).string_(i.asString).stringColor_(Color.gray(0.4)).font_(Font.monospace(14));
      trackButts = trackButts.add((
        mute: Button(view, Rect(21, 5, 25, 25)).states_([
          ["M", Color.gray(0.5), Color.gray(0.8)],
          ["M", Color.gray(0.7), Color.gray(0.3)]])
        .focusColor_(Color.clear).font_(Font.sansSerif(16, true)).action_({ |view|
          track.mute = view.value.asBoolean;
          timelineView.focus;
        }).value_(track.mute),
        solo: Button(view, Rect(48, 5, 25, 25)).states_([
          ["S", Color.gray(0.5), Color.gray(0.8)],
          ["S", Color.yellow, Color.black]])
        .focusColor_(Color.clear).font_(Font.sansSerif(16, true)).action_({ |view|
          track.solo = view.value.asBoolean;
          timelineView.focus;
        }).value_(track.solo),
      ));
      view;
    };
  }
}