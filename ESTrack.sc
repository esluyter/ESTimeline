ESTrack {
  var <clips, <mute, <solo = false;
  var <>timeline;
  var <isPlaying = false;
  var playRout;
  var dependantFunc;

  storeArgs { ^[clips, mute] }

  *new { |clips, mute = false|
    clips = clips ?? [];
    ^super.newCopyArgs(clips, mute).init;
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

  stop {
    playRout.stop;
    this.currentClips.do(_.stop);
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
            clip.play(offset, clock);
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
    var newClip = clip.deepCopy;
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

  mute_ { |val|
    mute = val;
    this.changed(\mute);
  }

  solo_ { |val|
    solo = val;
    this.changed(\solo);
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
}
