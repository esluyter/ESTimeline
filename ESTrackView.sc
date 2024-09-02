ESTrackView : UserView {
  var track;

  *new { |parent, bounds, track|
    ^super.new(parent, bounds).init(track);
  }

  init { |argtrack|
    var timelineView = this.parent;
    track = argtrack;

    this.drawFunc_({ |view|
      Pen.use {
        Pen.addRect(Rect(0, 0, this.bounds.width, 1));
        Pen.color_(Color.gray(0.7));
        Pen.fill;
        track.clips.reverse.do { |clip, j|
          if ((clip.startTime < timelineView.endTime) and: (clip.endTime > timelineView.startTime)) {
            // only draw clips in the timeline view bounds
            var left = timelineView.absoluteTimeToPixels(clip.startTime);
            var width = timelineView.relativeTimeToPixels(clip.duration);
            clip.draw(left, 3, width, this.bounds.height - 4);
          };
        };
        if (track.shouldPlay.not) {
          Pen.addRect(Rect(0, 0, this.bounds.width, this.bounds.height));
          Pen.color_(Color.gray(0.5, 0.25));
          Pen.fill;
        };
      };
    });

    this.acceptsMouse_(false);
  }
}