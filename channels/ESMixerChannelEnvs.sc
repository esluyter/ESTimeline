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
    };
    template.changed(\envs);
  }

  pan_ { |val|
    pan = val;
    if (pan.notNil) {
      pan.template = template;
    };
    template.changed(\envs);
  }

  *new { |level, pan, fx, preSends, postSends|
    fx = fx ?? [];
    preSends = preSends ?? [];
    postSends = postSends ?? [];
    ^super.newCopyArgs(level, pan, fx, preSends, postSends);
  }
}