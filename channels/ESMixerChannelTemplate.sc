ESMixerChannelTemplate {
  var <>inChannels, <>outChannels, <>level, <>pan, <>fx, <>preSends, <>postSends, <>envs;

  var <fxSynths;

  storeArgs { ^[inChannels, outChannels, level, pan, fx, preSends, postSends, envs]; }

  *new { |inChannels = 2, outChannels = 2, level = 1, pan = 0, fx, preSends, postSends, envs|
    fx = fx ?? [];
    preSends = preSends ?? [];
    postSends = postSends ?? [];
    envs = envs ?? ESMixerChannelEnvs();
    ^super.newCopyArgs(inChannels, outChannels, level, pan, fx, preSends, postSends, envs).init;
  }

  init {
    envs.template = this;
    fxSynths = [];
  }

  play { |startTime, clock, mc, duration|
    Server.default.bind {
      fxSynths = fx.collect { |fx|
        //mc.playfx(fx);
        mc.playfx(fx.playDefName, fx.prArgsValue(clock));
      };
    };

    envs.play(startTime, clock, mc, duration);
  }

  stop { |mc|
    //mc.effectgroup.release;
    fxSynths.do(_.release);
    fxSynths = [];
    envs.stop(mc);
  }
}