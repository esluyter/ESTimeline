ESRoutine : Routine {
  fastForward { |by, tolerance = 0, verbose = false|
    // fast forwards a routine by a certain amount
    var t = 0.0, delta;
    if (by <= 0) { ^0.0 };
    while { t.roundUp(tolerance) < by } {
      // delta is the amount of time to wait
      delta = this.next;
      if(delta.isNil) { if (verbose) { ("end of stream. Time left:" + (by - t)).postln; }; ^t - by };
      t = t + delta;
    };
    ^t - by; // time left to next event
  }
}