ESMidiListener {
  var ccFuncs, ccValues;

  *new {
    ^super.new.init;
  }

  init {
    MIDIClient.init;
    MIDIIn.connectAll;

    ccValues = 17.collect { 0.dup(127) };

    ccFuncs = 17.collect { |n|
      var chan = if (n == 16) { nil } { n };
      MIDIFunc.cc({ |val, num|
        ccValues[n][num] = val.linlin(0, 127, 0.0, 1.0);
      }, chan: chan).permanent_(true);
    };
  }

  ccValue { |num = 0, chan|
    chan = chan ?? 16;
    ^ccValues[chan][num];
  }

  free {
    ccFuncs.do(_.free);
  }
}