ESFxSynth : ESSynthClip {
  prep {
    if (doPlayFunc) {
      //func.asSynthDef(fadeTime: 0.001, name: this.autoDefName).add;
      /*
      var def = { // borrowed from ddwMixerChannel:
        var gate;
        var graph = SynthDef.wrap(func);
        if(UGen.buildSynthDef.allControlNames.includes(\gate).not) {
          gate = NamedControl.kr(\gate, 1);
          graph = graph * EnvGen.kr(Env.asr(0.01, 1, 0.02, 0), gate, doneAction: 2);
        };
      }.asSynthDef(outClass: \ReplaceOut, name: this.autoDefName);
      */
      Environment.new.use {
        var def = SynthDef.new(this.autoDefName, { |out|
          var result, rate, env;
          ~out = out;
          result = SynthDef.wrap(func).asUGenInput;
          rate = result.rate;
          if (rate.isNil or: { rate === \scalar }) {
            // Out, SendTrig, [ ] etc. probably a 0.0
            result
          } {
            var gate = NamedControl.kr(\gate, 1);
            result = result * EnvGen.kr(Env.asr(0.01, 1, 0.02, 0), gate, doneAction: 2);
            ReplaceOut.replaceZeroesWithSilence(result.asArray);
            ReplaceOut.multiNewList([rate, out]++result);
          };
        });
        def.add;
        //^def;
      };
    };
  }

  prArgsValue { |clock|
    var ret = [];
    args.pairsDo { |key, val|
      val = val.value;
      /*
      if (val.class == Symbol) { val = track.timeline[val]; };
      if (val.class == ESEnvClip) { val = val.bus.asMap; };
      //if (val.class == Bus) { val = val.asMap; };
      */
      ret = ret.add(key).add(val);
    };
    ^ret;
  }

  guiClass { ^ESFxSynthEditView }
  prHasTime { ^false }
  prHasColor { ^false }
}