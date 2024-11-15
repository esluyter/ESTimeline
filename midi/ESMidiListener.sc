ESMidiListener {
  var ccFuncs, ccValues;
  var bendFuncs, bendValues;
  var noteFuncs, noteValues, velValues;

  *new {
    ^super.new.init;
  }

  init {
    if (MIDIClient.initialized.not) {
      MIDIClient.init;
      MIDIIn.connectAll;
    };

    ccValues = 17.collect { 0.dup(127) };

    ccFuncs = 17.collect { |n|
      var chan = if (n == 16) { nil } { n };
      MIDIFunc.cc({ |val, num|
        ccValues[n][num] = val.linlin(0, 127, 0.0, 1.0);
      }, chan: chan).permanent_(true);
    };

    bendValues = 0.5.dup(17);

    bendFuncs = 17.collect { |n|
      var chan = if (n == 16) { nil } { n };
      MIDIFunc.bend({ |val|
        bendValues[n] = val.linlin(0, 16383, 0.0, 1.0);
      }, chan).permanent_(true);
    };

    noteValues = 0.5.dup(17);
    velValues = 0.5.dup(17);

    noteFuncs = 17.collect { |n|
      var chan = if (n == 16) { nil } { n };
      MIDIFunc.noteOn({ |vel, num|
        noteValues[n] = num.linlin(0, 127, 0.0, 1.0);
        velValues[n] = vel.linlin(0, 127, 0.0, 1.0);
      }, chan: chan).permanent_(true);
    };
  }

  ccValue { |num = 0, chan|
    chan = chan ?? 16;
    ^ccValues[chan][num];
  }

  bendValue { |chan|
    chan = chan ?? 16;
    ^bendValues[chan];
  }

  noteValue { |chan|
    chan = chan ?? 16;
    ^noteValues[chan];
  }

  velValue { |chan|
    chan = chan ?? 16;
    ^velValues[chan];
  }

  free {
    ccFuncs.do(_.free);
    bendFuncs.do(_.free);
    noteFuncs.do(_.free);
  }
}