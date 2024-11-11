ESDrawTimelineClip : ESDrawClip {
  prDraw { |left, top, width, height, editingMode, clipLeft, clipWidth, selected|
    var tracks = clip.timeline.tracks;
    var tratio = width / clip.duration;
    var pratio = tratio.reciprocal;
    var rulerHeight = 0;
    var trackHeight;
    var thisLeft;
    var division;

    /*
    left is the left side of the entire clip
    clipLeft is the leftmost visible edge
    */
    //[left, width, clipLeft, clipWidth].postln;

    clipLeft = clipLeft ?? left;
    clipWidth = clipWidth ?? width;


    Pen.use {
      Pen.addRect(Rect(clipLeft, top, clipWidth, height));
      Pen.clip;

      rulerHeight = ((height + 400) / 60).clip(10, 20);
      Pen.addRect(Rect(left, top, width, rulerHeight));
      Pen.fillColor = if (clip.useParentClock) { Color.gray(0.93) } { Color.white };
      Pen.strokeColor = Color.gray(0.3);
      Pen.width = 1;
      if (clip.useParentClock) {
        Pen.fill;
      } {
        Pen.fillStroke;
      };
      //
      // if (useParentClock.not) {
      //   Pen.addRect(Rect(left, top + rulerHeight, width, 1));
      //   Pen.color = Color.gray(0.8);
      //   Pen.fill;
      // };

      division = (60 / (width / clip.duration)).ceil;
      Pen.color = if (clip.useParentClock) { Color.gray(0.3, 0.3) } { Color.gray(0.3) };

      // TODO: fix this to only iterate over range above offset:
      (clip.offset + clip.duration + 1).asInteger.do { |i|
        if (i % division == 0) {
          if (i >= clip.offset) {
            thisLeft = ((i - clip.offset) * tratio) + left;
            Pen.addRect(Rect(thisLeft, top + 1, 1, rulerHeight));
            Pen.fill;
            Pen.stringAtPoint(i.asString, (thisLeft + 3)@(top + 1), Font.monospace(rulerHeight * (15/20)));
          };
        };
      };

      trackHeight = (height - rulerHeight) / tracks.size;
      tracks.do { |track, i|
        var thisTop = top + rulerHeight + (trackHeight * i) + 2;
        if (i > 0) {
          Pen.addRect(Rect(clipLeft, thisTop - 2, clipWidth, 1));
          Pen.color = Color.gray(0.5, 0.3);
          Pen.fill;
        };
        if (trackHeight > 8) {
          Pen.stringAtPoint(ESStringShortener.trim(track.mixerChannelName.asString, clipWidth - 5, Font.sansSerif(10)), (clipLeft + 5)@(thisTop + trackHeight - 13), Font.sansSerif(10), Color.gray(0.5));
        };
        track.clips.do { |thisClip|
          if ((thisClip.endTime > clip.offset) and: (thisClip.startTime < (clip.offset + clip.duration))) {
            var thisLeft = ((thisClip.startTime - clip.offset) * tratio) + left;
            var thisWidth = (thisClip.duration * tratio);
            var thisHeight = trackHeight - 3;

            // this stuff is all just for nested ESTimelineClips:
            var thisClipLeft = max(clipLeft, thisLeft);
            var thisClipWidth = thisWidth - (thisClipLeft - thisLeft);
            if ((thisClipLeft + thisClipWidth) > (clipLeft + clipWidth)) {
              thisClipWidth = (clipLeft + clipWidth) - thisClipLeft;
            };

            thisClip.drawClip.draw(thisLeft, thisTop, thisWidth, thisHeight, false, thisClipLeft, thisClipWidth, selected, false, nil, nil, thisClip.duration);
          };
        };
      };

      /*
      if (clip.timeline.isPlaying and: clip.useParentClock.not) {
        // sounding playhead in black
        thisLeft = ((clip.timeline.soundingNow - clip.offset) * tratio) + left;
        Pen.addRect(Rect(thisLeft, top, 2, height));
        Pen.color = Color.black;
        Pen.fill;

        // "scheduling playhead" in gray
        Pen.color = Color.gray(0.5, 0.5);
        thisLeft = ((clip.timeline.now - clip.offset) * tratio) + left;
        Pen.addRect(Rect(thisLeft, top, 2, height));
        Pen.fill;
      };
      */
      Pen.addRect(Rect(left, top + rulerHeight + 1, width, height - rulerHeight - 1));
      Pen.strokeColor = if (clip.timeline.useEnvir) { Color.gray(0.4) } { Color.gray(0.8) };
      Pen.width = 1;
      Pen.stroke;
    };
  }
}