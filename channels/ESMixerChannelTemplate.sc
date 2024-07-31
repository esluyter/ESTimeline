ESMixerChannelTemplate {
  var <>inChannels, <>outChannels, <>level, <>pan, <>fx, <>preSends, <>postSends, <>envs;

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
  }

  play { |startTime, clock, mc, duration|
    fx.do { |fx|
      //mc.playfx(fx);
      mc.playfx(fx.playDefName, fx.prArgsValue(clock));
    };

    envs.play(startTime, clock, mc, duration);
  }

  stop { |mc|
    mc.effectgroup.release;
    envs.stop(mc);
  }
}