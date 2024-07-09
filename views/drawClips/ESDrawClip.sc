ESDrawClip {
  var clip;

  *new { |clip|
    ^super.newCopyArgs(clip);
  }

  // draw this clip on a UserView using Pen
  draw { |left, top, width, height, editingMode = false, clipLeft, clipWidth, selected = false, drawBorder = true| // these last two are for ESTimelineClips
    var font = Font("Helvetica", 14, true);

    if (clip.track.shouldPlay) {
      Pen.color = clip.color(selected, editingMode).alpha_(if (clip.mute) { if (selected) { 0 } { 0.1 } } { 1.0 });
    } {
      Pen.color = Color.white.lighten(clip.color(selected, editingMode), 0.5).alpha_(if (clip.mute) { if (selected) { 0 } { 0.2 } } { 1.0 });
    };
    Pen.strokeColor = Color.cyan;
    Pen.width = 2;
    Pen.addRect(Rect(left, top, width, height));
    if (selected and: drawBorder) { Pen.fillStroke } { Pen.fill };

    if (clip.mute) {
      var title = ESStringShortener.trim(clip.prTitle, width - 3.5, font);
      Pen.stringAtPoint(title, (left + 3.5)@(top + 2), font, clip.color.alpha_(0.3));
    } {
      if (editingMode and: clip.hasEditingMode) {
        Pen.addRect(Rect(left, top, width, height));
        Pen.strokeColor = Color.white;
        Pen.width = 3;
        Pen.stroke;
      };

      Pen.color = Color.gray(0.8, 0.5);
      Pen.addRect(Rect(left + width - 1, top, 1, height));
      Pen.fill;

      // if it's more than 5 pixels wide and high, call the prDraw function
      if ((width > 5) and: (height > 10)) {
        var title;
        try {
          if (clip.track.timeline.useEnvir) {
            clip.track.timeline.envir.use {
              ~thisTimeline = clip.track.timeline;
              title = this.prDraw(left, top, width, height, editingMode, clipLeft, clipWidth, selected, drawBorder);
            }
          } {
            ~thisTimeline = clip.track.timeline;
            title = this.prDraw(left, top, width, height, editingMode, clipLeft, clipWidth, selected, drawBorder);
          };
        } {
          title = ""
        };

        if (clip.name.notNil and: (clip.class != ESClip)) { title = clip.name.asCompileString ++ " (" ++ title ++ ")" };

        if (left < 0) {
          width = width + left;
          left = 0;
        };
        if (clipLeft.notNil and: { left < clipLeft }) {
          width = width - (clipLeft - left);
          left = clipLeft;
        };
        title = ESStringShortener.trim(title, width - 3.5, font);
        Pen.stringAtPoint(title, (left + 3.5)@(top + 2), font, Color.gray(1, 0.6));
      };
    };
  }

  // override in subclass
  prHover { |x, y, hoverTime, left, top, width, height| }
  prHoverLeave {}
  prMouseMove { |x, y, xDelta, yDelta, mods, left, top, width, height| }
  prMouseDown { |x, y, mods, buttNum, clickCount, left, top, width, height| }
  prDraw { |left, top, width, height, editingMode, clipLeft, clipWidth, selected, drawBorder|
    // default clip is a comment clip
    var lines = clip.comment.split($\n);
    var font = Font.sansSerif(14);
    var strTop;
    if (left < 0) {
      width = width + left;
      left = 0;
    };
    lines = [if (clip.name.isNil) { "" } { clip.name.asString }] ++ lines;
    lines.do { |line, i|
      var thisFont = if (i > 0) { font } { font.copy.size_(17) };
      line = ESStringShortener.trim(line, width - 5, thisFont);
      strTop = (2+(i * 16));
      if (strTop < height) {
        Pen.stringAtPoint(line, (left+3.5)@(strTop + top), thisFont, Color.gray(0.0, 0.7));
      };
    };
    Pen.color = Color.gray(0.7);
    Pen.addRect(left, top, width, height);
    Pen.stroke;
    // expected to return title of clip to be drawn
    ^clip.prTitle;
  }
}