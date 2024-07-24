ESTrack {
  var <clips, <mute, <name, <useMixerChannel, <solo = false, <heightMultiplier = 1;
  var <>timeline;
  var <isPlaying = false;
  var playRout;
  var dependantFunc;

  totalHeightMultiplier {
    var template = this.mixerChannelTemplate;
    // note: this isn't right, but just doing this to have something to test with
    var envHeight = (template.fx.size + template.preSends.size + template.postSends.size) * timeline.envHeightMultiplier;
    ^heightMultiplier + envHeight;
  }

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
      a.endTime < b.endTime;
    };
  }

  currentClips {
    // returns all playing clips
    ^clips.select(_.isPlaying);
  }

  nowClips {
    // returns all clips at "now", whether or not playing
    ^clips.select({ |clip| ((clip.startTime <= timeline.now) and: (clip.endTime >= timeline.now)) });
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
      // iterate over all the clips
      clips.copy.sort({ |a, b| a.startTime < b.startTime }).do { |clip|
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

  mixerChannelName { // this will either be an integer or a symbol
    ^if (name.isNil) { this.index } { name.asSymbol };
  }

  mixerChannel {
    ^timeline.mixerChannels[this.mixerChannelName];
  }

  mixerChannelTemplate {
    ^timeline.mixerChannelTemplates[this.mixerChannelName] ?? timeline.defaultMixerChannelTemplate;
  }

  /*
  inChannels { ^if (this.mixerChannel.notNil) { this.mixerChannel.inChannels } { nil } }
  outChannels { ^if (this.mixerChannel.notNil) { this.mixerChannel.outChannels } { nil } }
  level { ^if (this.mixerChannel.notNil) { this.mixerChannel.level } { nil } }
  pan { ^if (this.mixerChannel.notNil) { this.mixerChannel.pan } { nil } }

  level_ { |val|

  }
  */

  name_ { |val|
    var mcName = this.mixerChannelName;
    var mcTemplate = timeline.mixerChannelTemplates[mcName];
    var index = this.index;

    timeline.mixerChannelTemplates[mcName] = nil;

    name = val;

    timeline.mixerChannelTemplates[this.mixerChannelName] = mcTemplate;

    timeline.initMixerChannels;
    this.changed(\name, val);
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
