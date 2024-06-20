ESTrackView : UserView {
  var track;

  *new { |parent, bounds, track|
    ^super.new(parent, bounds).init(track);
  }

  init { |argtrack|
    var timelineView = this.parent;
    track = argtrack;

    this.drawFunc_({ |view|
      var timeline = track.timeline;
      Pen.use {
        Pen.addRect(Rect(0, 0, this.bounds.width, 1));
        Pen.color_(Color.gray(0.7));
        Pen.fill;
        track.clips.reverse.do { |clip, j|
          if ((clip.startTime < timelineView.endTime) and: (clip.endTime > timelineView.startTime)) {
            // only draw clips in the timeline view bounds
            clip.draw(*timelineView.clipBounds(clip));
          };
        };
        if (track.shouldPlay.not) {
          Pen.addRect(Rect(0, 0, this.bounds.width, this.bounds.height));
          Pen.color_(Color.gray(0.5, 0.25));
          Pen.fill;
        };
        if (timeline.parentClip.notNil) {
          if (timeline.parentClip.offset > timelineView.startTime) {
            Pen.addRect(Rect(0, 0, timelineView.absoluteTimeToPixels(timeline.parentClip.offset), this.bounds.height));
            Pen.color = (Color.gray(0.5, 0.5));
            Pen.fill;
          };
          if (timeline.parentClip.offset + timeline.parentClip.duration < timelineView.endTime) {
            var left = timelineView.absoluteTimeToPixels(timeline.parentClip.offset + timeline.parentClip.duration);
            Pen.addRect(Rect(left, 0, this.bounds.width, this.bounds.height));
            Pen.color = (Color.gray(0.5, 0.5));
            Pen.fill;
          };
        };
      };
    });

    this.acceptsMouse_(false);
  }
}