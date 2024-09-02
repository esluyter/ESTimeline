ESTimelineClip : ESClip {
  var <timeline, <useParentClock;

  useParentClock_ { |val|
    useParentClock = val;
    timeline.changed(\useParentClock, val);
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
    timeline.clips.do { |clip|
      if (clip.class == ESTimelineClip) {
        clip.initMixerChannels;
      };
    };
  }

  update { |argTimeline, what, val|
    this.changed(\timeline, [what, val]);
  }

  play { |startOffset = 0.0, clock, maxDuration = inf|
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

      // wait the appropriate time, then stop
      min(duration - startOffset, maxDuration).wait;
      this.stop;
    }.fork(clock);
  }

  prStop { |hard = false|
    timeline.stop(hard);
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