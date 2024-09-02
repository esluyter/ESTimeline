ESDrawPatternClip : ESDrawClip {
  var >drawData;

  // data to be drawn on the clip
  drawData {
    var stream, t;

    // return cached data if it exists
    if (drawData.asArray.size > 0) {
      ^drawData;
    };

    // otherwise calculate it
    stream = clip.patternStream;
    drawData = [];
    t = 0.0;
    // keep adding events until we're past the duration (or the stream has run out)
    while { t < clip.duration } {
      // use default Event as the proto event (we need this to be sure of calculating the keys later)
      var event = stream.next(Event.default);
      if (event.notNil) {
        // if the stream has not run out:
        var simpleEvent = event.use {
          (
            instrument: event.instrument,
            freq: event.freq,
            amp: event.amp,
            sustain: event.sustain,
            dur: event.dur,
            // because it doesn't work to specify the isRest parameter directly, as
            // mentioned in part 0
            restdummy: if (event.isRest) { Rest() } { 1 }
          )
        };
        drawData = drawData.add(simpleEvent);
        t = t + simpleEvent.dur;
      } {
        // if the stream has run out:
        t = inf;
      };
    };
    ^drawData;
  }

  prDraw { |left, top, width, height|
    var t = 0.0;
    var instrument = Set(8);

    /*
    var string = this.patternString;
    var lines = string.split($\n);
    var font = Font.monospace(10);
    lines.do { |line, i|
      line = ESStringShortener.trim(line, width - 5, font);
      Pen.stringAtPoint(line, (left+3.5)@(top+20+(i * 10)), font, Color.gray(1.0, 0.4));
    };
    */

    //try {
      this.drawData.do { |event|
        if (event.isRest.not) {
          var x = left + (t * width / clip.duration);
          var eventHeight = 2;
          event.freq.asArray.do { |freq, i|
            var eventWidth, y, amp;
            amp = event.amp.asArray.wrapAt(i);
            if (amp.isNumber.not) { amp = 0.1 };
            if (freq.isNumber.not) { freq = 500 };
            eventWidth = event.sustain.asArray.wrapAt(i) * width / clip.duration;
            y = freq.explin(20, 20000, top + height, top);
            Pen.color = Color.gray(1, amp.ampdb.linexp(-60.0, 0.0, 0.05, 1.0));
            Pen.addRect(Rect(x, y, eventWidth, eventHeight));
            Pen.fill;
          };
          instrument.add(event.instrument.asString);
        };
        t = t + event.dur;
      };
      // return the title of the clip
      ^clip.prTitle(instrument.asArray.join(" / ") ++ ": " ++ clip.pattern.class);
    /*
    } {
      ^clip.prTitle;
    };
    */
  }
}