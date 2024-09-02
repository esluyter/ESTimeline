ESEnvClip : ESClip {
  var <env, <bus;
  var <synth;

  env_ { |val| env = val; this.changed(\env); }
  bus_ { |val| bus = val; this.changed(\bus); }
  rate { ^this.bus.value.rate }

  storeArgs { ^[startTime, duration, env, bus, color, offset] }

  *new { |startTime, duration, env, bus, color, offset|
    ^super.new(startTime, duration, color).init(env, bus, offset);
  }

  init { |argEnv, argBus, argOffset|
    env = argEnv;
    bus = argBus;
    offset = argOffset;

    ServerBoot.add {
      SynthDef('ESEnvClip_internal_kr', { |out, gate = 1, tempo = 1|
        var env = \env.kr(Env(0.dup(1000), 1.dup(999), 0.dup(999)));
        var sig = EnvGen.kr(env, timeScale: tempo.poll.reciprocal);
        FreeSelf.kr(gate <= 0);
        Out.kr(out, sig);
      }).add;
      SynthDef('ESEnvClip_internal_ar', { |out, gate = 1, tempo = 1|
        var env = \env.kr(Env(0.dup(1000), 1.dup(999), 0.dup(999)));
        var sig = EnvGen.ar(env, timeScale: tempo.reciprocal);
        FreeSelf.kr(gate <= 0);
        Out.ar(out, sig);
      }).add;
    };
  }

  prStop {
    Server.default.bind { synth.release };
    synth = nil;
  }

  prStart { |startOffset = 0.0, clock|
    var defName = if (this.rate == 'control') { 'ESEnvClip_internal_kr' } { 'ESEnvClip_internal_ar' };
    Server.default.bind {
      synth = Synth(defName, [env: this.envToPlay, out: bus.value, tempo: track.timeline.tempo.postln]);
      //synth = Synth(defName.value, this.prArgsValue(clock), target.value, addAction.value)
    };
  }

  prDraw { |left, top, width, height|
    var pratio = duration / width;
    var line = this.busString ++ "  -> " ++ bus.value.asCompileString;
    var font = Font.monospace(10);
    Pen.use {
      Pen.moveTo(left@(top + ((1 - env[offset]) * height)));
      width.do { |i|
        Pen.lineTo((left + i)@(top + ((1 - env[offset + (i * pratio)]) * height)));
      };
      Pen.color = Color.gray(1.0, 0.5);
      Pen.stroke;
    };
    while { max(0, width - 5) < (QtGUI.stringBounds(line, font).width) } {
      if (line.size == 1) {
        line = "";
      } {
        line = line[0..line.size-2];
      };
    };
    Pen.stringAtPoint(line, (left+3.5)@(top+20+(0 * 10)), font, Color.gray(1.0, 0.8));
    ^"Env"
  }

  defaultColor { ^Color.hsv(0.58, 0.4, 0.7, 0.7) }

  guiClass { ^nil }

  busString {
    if (bus.isFunction) {
      var cs = bus.asCompileString;
      if (cs.size == 2) { ^"" };
      ^cs[1..cs.size-2];
    };
    ^bus.asCompileString;
  }

  envToPlay { |startOffset = 0|
    var playOffset = offset + startOffset;
    var thisEnv = env.copy;
    var initlevel = env[playOffset];

    if (playOffset < 0) {
      thisEnv = thisEnv.delay(playOffset * -1);
    };
    if (playOffset > 0) {
      var runningtime = 0.0;
      var i = 0;
      playOffset = playOffset * -1;
      while { (runningtime > playOffset) && (i < thisEnv.times.size) } {
        runningtime = runningtime - thisEnv.times[i];
        i = i + 1;
      };
      if (runningtime <= playOffset) { // passed target
        var timetokeep = playOffset - runningtime;
        thisEnv.levels = thisEnv.levels[(i - 1)..thisEnv.levels.size];
        thisEnv.times = thisEnv.times[(i - 1).thisEenv.times.size];
        thisEnv.curves = if (thisEnv.curves.isArray) { thisEnv.curves[(i - 1)..thisEnv.curves.size] } { thisEnv.curves };
        thisEnv.times[0] = timetokeep;
        thisEnv.levels[0] = initlevel;
      } { // ran out of envelope
        thisEnv.times = [0];
        thisEnv.levels = initlevel ! 2;
        thisEnv.curves = \lin;
      };
    };

    ^thisEnv;
  }
}