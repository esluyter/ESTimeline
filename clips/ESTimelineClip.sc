ESTimelineClip : ESClip {
  var <timeline, <useParentClock;
  var <stopRout;

  useParentClock_ { |val|
    useParentClock = val;
    timeline.changed(\useParentClock, val);
  }

  track_ { |val|
    track = val;
    this.initMixerChannels;
  }

  storeArgs { ^[startTime, duration, offset, color, name, timeline, useParentClock, mute] }

  *new { |startTime, duration, offset = 0, color, name, timeline, useParentClock = true, mute = false|
    ^super.new(startTime, duration, offset, color, name, mute: mute).init(timeline, useParentClock);
  }

  init { |argTimeline, argUseParentClock|
    timeline = argTimeline;
    useParentClock = argUseParentClock;
    timeline.parentClip = this;
    timeline.addDependant(this);
  }

  initMixerChannels {
    timeline.initMixerChannels;
  }

  refreshChildNow {
    timeline.now_(track.timeline.now - startTime + offset, propagate: false);
    timeline.clips.select({ |clip| clip.class == ESTimelineClip }).do(_.refreshChildNow);
  }

  refreshParentNow {
    track.timeline.now_(timeline.now + startTime - offset, propagate: false);
    if (track.timeline.parentClip.notNil) {
      track.timeline.parentClip.refreshParentNow;
    };
  }

  update { |argTimeline, what, val|
    this.changed(\timeline, [what, val]);
  }

  play { |startOffset = 0.0, clock, maxDuration = inf|
    var waitTime = min(duration - startOffset, maxDuration);

    clock = timeline.prMakeClock;

    // stop if we're playing
    if (isPlaying) {
      this.stop;
    };

    // set isPlaying to true
    isPlaying = true;

    // play the clip on a new Routine on this clock
    playRout = {
      // start the clip from specified start offset
      this.prStart(startOffset, clock);

      // wait the appropriate time, then stop if there hasn't been a goto
      waitTime.wait;
      if (timeline.now.fuzzyEqual((offset + duration), 0.1).asBoolean) {
        this.stop;
      };
    }.fork(clock);

    // make sure clip stops when parent timeline reaches end
    stopRout = {
      waitTime.wait;
      if (isPlaying) {
        this.stop;
      };
    }.fork(track.timeline.playClock);
  }

  prStop { |hard = false|
    timeline.stop(hard);
    stopRout.stop; // playRout has already been stopped
  }

  prStart { |startOffset = 0.0, clock|
    startOffset = startOffset + offset;

    timeline.play(startOffset, makeClock: false);
  }

  prTempoChanged { |tempo|
    // this is called when parent timeline's tempo changes
    if (useParentClock) {
      timeline.currentClips.do(_.prTempoChanged(tempo));
    };
  }

  prTitle { ^"Timeline" }

  prHasOffset { ^true } // whether to show offset parameter for editing

  defaultColor { ^if (timeline.useEnvir) { Color.gray(0.96, 0.5) } { Color.clear  } }

  guiClass { ^ESTimelineClipEditView }
  drawClass { ^ESDrawTimelineClip }

  prFree {
    timeline.free;
  }
}