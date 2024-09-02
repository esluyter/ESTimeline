ESDrawRoutineClip : ESDrawClip {
  prDraw { |left, top, width, height, editingMode, clipLeft, clipWidth|
    if (left < 0) {
      width = width + left;
      left = 0;
    };
    if (clipLeft.notNil and: { left < clipLeft }) {
      width = width - (clipLeft - left);
      left = clipLeft;
    };

    if ((height > 30) and: (width > 15)) {
      var string = clip.func.asESDisplayString;
      var lines = string.split($\n);
      var font = Font.monospace(10);
      var funcHeight = lines.size * 10;
      var strTop;
      lines.do { |line, i|
        line = ESStringShortener.trim(line, width - 5, font);
        strTop = 20 + (i * 10);
        if (strTop < height) {
          Pen.stringAtPoint(line, (left+3.5)@(top+strTop), font, Color.gray(1.0, 0.6));
        };
      };
      if (25 + funcHeight < height) {
        Pen.addRect(Rect(left, top + 25 + funcHeight, width / 2, 1));
        Pen.color = Color.gray(1.0, 0.15);
        Pen.fill;
      };
      string = clip.stopFunc.asESDisplayString;
      lines = string.split($\n);
      lines.do { |line, i|
        line = ESStringShortener.trim(line, width - 5, font);
        strTop = 30 + funcHeight + (i * 10);
        if (strTop < height) {
          Pen.stringAtPoint(line, (left+3.5)@(top+strTop), font, Color.gray(1.0, 0.4));
        };
      };
    };
    ^clip.prTitle;
  }
}