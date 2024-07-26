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
          try {
            if ((clip.startTime < timelineView.endTime) and: (clip.endTime > timelineView.startTime)) {
              // only draw clips in the timeline view bounds
              clip.drawClip.draw(*timelineView.clipBounds(clip));
            };
          };
        };
        // draw envelope area
        if (track.envs.size > 0) {
          Pen.addRect(Rect(0, timelineView.trackHeight, this.bounds.width, this.bounds.height - timelineView.trackHeight));
          Pen.color = Color.gray(0.8, 0.2);
          Pen.fill;
        };
        track.envs.do { |val, i|
          var envHeight = timelineView.trackHeight * timeline.envHeightMultiplier;
          //[key, val, i, envHeight].postln;
          Pen.addRect(Rect(0, timelineView.trackHeight + (envHeight * i), this.bounds.width, 1));
          Pen.color = Color.gray(0.8);
          Pen.fill;

          // draw envelope
          {
            var width = this.bounds.width;
            var height = envHeight - 2;
            var pratio = timelineView.duration / width;
            var tratio = pratio.reciprocal;
            //var minString = clip.min.asString;
            //var maxString = clip.max.asString;
            //var minWidth, maxWidth;
            //var font = Font.monospace(10);
            var n = (width / (envHeight / 50).max(1)).asInteger;
            var nratio = width / n;
            var firstI, lastI, prevY;
            var thisEnv = val.value.env;
            var top = timelineView.trackHeight + (envHeight * i) + 1;
            var left = 0;
            var points = ESDrawEnvClip.envBreakPoints(thisEnv, (0 - timelineView.startTime) * tratio, top, width, height, pratio, tratio);
            firstI = ((0 - left) / nratio).asInteger.clip(0, n);
            lastI = ((Window.screenBounds.width - left) / nratio).asInteger.clip(0, n);
            prevY = top + ((1 - thisEnv[timelineView.startTime + (firstI * pratio)]) * height);
            (firstI..lastI).do { |i|
              var thisY, thisX;
              i = i * nratio;
              thisX = left + i;
              thisY = top + ((1 - thisEnv[timelineView.startTime + (i * pratio)]) * height);
              Pen.addRect(Rect(thisX, min(prevY, thisY), nratio, max(1, abs(prevY - thisY))));
              prevY = thisY;
            };
            Pen.color = Color.gray(0.6);
            Pen.fill;

            // draw breakpoints
            points.do { |point, i|
              //Pen.addOval(Rect(point.x - 2.5, point.y - 2.5, 6, 6));
              //Pen.strokeColor = Color.gray(0.9);
              Pen.addOval(Rect(point.x - 1.5, point.y - 1.5, 4, 4));
              //if (i == hoverIndex) {
                //var val = clip.valueAtIndex(i);
                //Pen.stringAtPoint(val.asString, point.x@(if (point.y - 20 < top) { point.y + 20 } { point.y - 20 }), Font.sansSerif(15), Color.white);
                //Pen.fillColor = Color.white;
              //} {
                Pen.fillColor = Color.gray(0.5);
              //};
              Pen.width = 2;
              //Pen.fillStroke;
              Pen.fill;
            };
          }.value;
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