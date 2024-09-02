ESMixerChannelEnvs {
  var <level, <pan, <fx, <preSends, <postSends;
  var <template;

  template_ { |val|
    template = val;
    ([level, pan] ++ fx ++ preSends ++ postSends).do { |env|
      if (env.notNil) {
        env.template = val;
      };
    };
  }

  level_ { |val|
    level = val;
    if (level.notNil) {
      level.template = template;
      level.name = "level";
    };
    template.changed(\envs);
  }

  pan_ { |val|
    pan = val;
    if (pan.notNil) {
      pan.template = template;
      pan.name = "pan";
    };
    template.changed(\envs);
  }

  storeArgs { ^[level, pan, fx, preSends, postSends]; }

  *new { |level, pan, fx, preSends, postSends|
    fx = fx ?? [];
    preSends = preSends ?? [];
    postSends = postSends ?? [];
    ^super.newCopyArgs(level, pan, fx, preSends, postSends).init;
  }

  init {
    if (level.notNil) {
      level.template = template;
      level.name = "level";
    };
    if (pan.notNil) {
      pan.template = template;
      pan.name = "pan";
    };
    preSends.do { |env, i|
      if (env.notNil) {
        env.template = template;
        env.name = "pre_" ++ i;
      };
    };
    postSends.do { |env, i|
      if (env.notNil) {
        env.template = template;
        env.name = "post_" ++ i;
      };
    };
    fx.do { |ev, i|
      if (ev.notNil) {
        ev.keysValuesDo { |param, env|
          if (env.notNil) {
            env.template = template;
            env.name = "fx_" ++ i ++ "_" ++ param;
          };
        };
      };
    };
  }

  play { |startTime, clock, mc, duration|
    if (pan.notNil) {
      pan.playPan(startTime, clock, mc, duration);
    };
    if (level.notNil) {
      level.playLevel(startTime, clock, mc, duration);
    };
  }

  stop { |mc|
    if (pan.notNil) {
      pan.stop;
      mc.stopAuto(\pan);
    };
    if (level.notNil) {
      level.stop;
      mc.stopAuto(\level);
    };
  }
}