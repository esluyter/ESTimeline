ESClip {
  var <startTime, <duration, <offset, >color, <name, <comment, <mute;
  var <>track;
  var <isPlaying = false;
  var playRout;
  var <drawClip; // handles drawing on TimelineView

  var <id;
  classvar nextId = 0;

  storeArgs { ^[startTime, duration, offset, color, name, comment, mute] }

  *new { |startTime, duration, offset = 0, color, name, comment = "", mute = false|
    ^super.newCopyArgs(startTime, duration, offset, color, name, comment, mute).makeDrawClip;
  }

  makeDrawClip {
    drawClip = this.drawClass.new(this);
    id = nextId;
    nextId = nextId + 1;
  }

  mute_ { |val| mute = val; this.changed(\mute, val) }

  startTime_ { |val, adjustOffset = false|
    //val = max(val, 0);
    if (adjustOffset) {
      var delta;
      val = min(val, this.endTime);
      delta = val - startTime;
      offset = offset + delta;
      duration = duration - delta;
    };
    startTime = val;
    this.changed(\startTime, val);
  }

  endTime_ { |val|
    duration = max(val - startTime, 0);
    this.changed(\duration, duration);
  }

  duration_ { |val|
    duration = val;
    this.changed(\duration, val);
  }

  offset_ { |val|
    offset = val;
    this.changed(\offset, val);
  }

  name_ { |val|
    if (val.asString.size > 0) {
      name = val;
    } {
      name = nil;
    };
    this.changed(\name, val);
  }

  comment_ { |val|
    comment = val;
    this.changed(\comment, val);
  }

  color { |selected = false, editingMode = false|
    var ret = color ?? { this.defaultColor };
    if (selected) {
      ret = Color.white.lighten(ret, 0.5);
    };
    if (editingMode) {
      if (this.drawClip.hasEditingMode) {
        ret = Color.black.darken(ret, 0.5);
      } {
        ret = ret.alpha_(ret.alpha * 0.5);
      }
    };
    ^ret;
  }

  rawColor {
    ^color
  }

  stop { |hard = false|
    // stop the clip
    this.prStop(hard);
    isPlaying = false;
    // in case of premature stop:
    playRout.stop;
  }

  play { |startOffset = 0.0, clock|
    // default to play on default TempoClock
    clock = clock ?? TempoClock.default;

    // stop if we're playing
    if (isPlaying) {
      this.stop;
    };

    if (mute.not) {
      // set isPlaying to true
      isPlaying = true;

      // play the clip on a new Routine on this clock
      playRout = {
        // set "thisTimeline" environment var
        ~thisTimeline = track.timeline;

        // start the clip from specified start offset
        this.prStart(startOffset, clock);

        // wait the appropriate time, then stop
        (duration - startOffset).wait;
        this.stop;
      }.fork(clock);
    };
  }

  free {
    this.cleanup;
    this.prFree;
    this.release;
  }

  // override these in subclasses
  prep {}
  cleanup {}
  prFree { }
  prStart { }
  prStop { }
  prTitle { ^"" } // title to display (not including name)
  prHasOffset { ^false } // whether to show offset parameter for editing
  prHasTime { ^true } // all timeline clips should have true, fx clips are false
  prHasColor { ^true } // currently fx clips are false
  prTempoChanged { |tempo| } // so far this is just so env clips follow tempo changes
  defaultColor { ^Color.gray(1); }
  guiClass { ^ESClipEditView }
  drawClass { ^ESDrawClip }

  // helper methods
  endTime { ^startTime + duration }

  duplicate {
    //this.asCompileString.interpret.track_(track);
    //this.class.new(*this.storeArgs).track_(track);
    ^Object.fromESArray(this.asESArray).track_(track).prep;
  }

  index {
    ^track.clips.indexOf(this);
  }

}
