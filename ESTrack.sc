ESTrack {
  var <clips, <mute, <name, <useMixerChannel, <solo = false;
  var <>timeline;
  var <isPlaying = false;
  var playRout;
  var dependantFunc;

  storeArgs { ^[clips, mute, name, useMixerChannel] }

  *new { |clips, mute = false, name, useMixerChannel = true|
    clips = clips ?? [];
    ^super.newCopyArgs(clips, mute, name, useMixerChannel).init;
  }

  init {
    // forward changed messages from clips
    dependantFunc = { |theClip, what, value|
      this.changed(\clip, [clips.indexOf(theClip), theClip, what, value].flat);
    };
    clips.do { |clip, i|
      clip.track = this;
      clip.addDependant(dependantFunc);
    };
  }

  sortClips {
    // good to call this before assuming they are ordered
    clips.sort { |a, b|
      a.startTime < b.startTime
    };
  }

  currentClips {
    // returns all playing clips
    ^clips.select(_.isPlaying)
  }

  stop { |hard = false|
    playRout.stop;
    this.currentClips.do(_.stop(hard));
  }

  play { |startTime = 0.0, clock|
    // default to play on default TempoClock
    clock = clock ?? TempoClock.default;
    // stop if we're playing
    if (this.isPlaying) { this.stop };
    // play the track on a new Routine on this clock
    playRout = {
      // the variable "t" tracks the current hypothetical playhead
      // as we are scheduling these events
      // it begins at our startTime
      var t = startTime;
      // make sure clips are in order
      this.sortClips;
      // iterate over all the clips
      clips.do { |clip|
        if (clip.endTime < t) {
          // skip all clips previous to t, i.e. startTime
        } {
          // how far into the clip do we need to start
          var offset = max(0, t - clip.startTime);
          // the playhead time we will start playing
          var startTime = clip.startTime + offset;
          // wait the appropriate amount of time
          (startTime - t).wait;
          // ...and play the clip
          if (this.shouldPlay) {
            if (timeline.parentClip.notNil) {
              var maxDuration = timeline.parentClip.offset + timeline.parentClip.duration - t;
              clip.play(offset, clock, maxDuration);
            } {
              clip.play(offset, clock);
            };
          };
          // adjust t to the current time
          t = startTime;
        };
      };
    }.fork(clock);
  }

  addClip { |clip|
    clip.track = this;
    clip.addDependant(dependantFunc);
    clips = clips.add(clip);
    this.sortClips;
    this.changed(\clips);
  }

  removeClip { |index, doFree = true|
    var clip = clips.removeAt(index);
    if (doFree) {
      clip.free;
    };
    this.changed(\clips);
  }

  splitClip { |index, time|
    var clip = clips[index];
    var newClip = clip.duplicate;
    newClip.track = this;
    clip.endTime = time;
    newClip.startTime_(time, true);
    newClip.addDependant(dependantFunc);
    clips = clips.add(newClip);
    this.sortClips;
    this.changed(\clips);
  }

  free {
    clips.do(_.free);
    this.release;
  }

  mixerChannelName {
    ^if (name.isNil) { this.index.asSymbol } { name.asSymbol };
  }

  name_ { |val|
    name = val;
    timeline.initMixerChannels;
    this.changed(\name);
  }

  mute_ { |val|
    mute = val;
    this.changed(\mute);
  }

  solo_ { |val|
    solo = val;
    this.changed(\solo);
  }

  useMixerChannel_ { |val|
    useMixerChannel = val;
    timeline.initMixerChannels;
    this.changed(\useMixerChannel)
  }

  shouldPlay {
    if (timeline.hasSolo) {
      if (solo) {
        ^true;
      } {
        ^false;
      };
    };
    ^mute.not;
  }

  index {
    ^timeline.tracks.indexOf(this);
  }

  clipsInRange { |timeA, timeB|
    var thisStartTime, thisEndTime;
    var ret = [];
    #thisStartTime, thisEndTime = [timeA, timeB].sort;
    clips.do { |clip|
      if ((clip.startTime < thisEndTime) and: (clip.endTime > thisStartTime)) {
        ret = ret.add(clip);
      };
    };
    ^ret;
  }

  insertTime { |timeA, timeB|
    var thisStartTime, thisEndTime, thisDuration;
    #thisStartTime, thisEndTime = [timeA, timeB].sort;
    thisDuration = thisEndTime - thisStartTime;
    clips.copy.do { |clip|
      if ((clip.startTime < thisStartTime) and: (clip.endTime > thisStartTime)) {
        this.splitClip(clip.index, thisStartTime);
      };
    };
    clips.do { |clip|
      if (clip.startTime >= thisStartTime) {
        clip.startTime = clip.startTime + thisDuration;
      };
    };
  }

  deleteTime { |timeA, timeB|
    var thisStartTime, thisEndTime, thisDuration;
    #thisStartTime, thisEndTime = [timeA, timeB].sort;
    thisDuration = thisEndTime - thisStartTime;
    clips.copy.do { |clip|
      if ((clip.startTime < thisStartTime) and: (clip.endTime > thisStartTime)) {
        this.splitClip(clip.index, thisStartTime);
      };
      if ((clip.startTime < thisEndTime) and: (clip.endTime > thisEndTime)) {
        this.splitClip(clip.index, thisEndTime);
      };
    };
    clips.copy.do { |clip|
      if (clip.startTime >= thisStartTime) {
        if (clip.startTime < thisEndTime) {
          this.removeClip(clip.index);
        } {
          clip.startTime = clip.startTime - thisDuration;
        };
      };
    };
  }
}
