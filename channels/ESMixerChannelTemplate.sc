ESMixerChannelTemplate {
  var <>inChannels, <>outChannels, <>level, <>pan, <>fx, <>preSends, <>postSends, <>envs;

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
}