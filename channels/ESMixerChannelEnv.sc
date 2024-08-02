ESMixerChannelEnv {
  var <env, <min, <max, <curve, <isExponential;
  var <hoverIndex, <editingFirst, <originalCurve, <curveIndex;
  var left, top, width, height, pratio, tratio, envHeight, startTime;
  var <>template;
  var <>name;
  var <synth, playRout, <bus;

  hoverIndex_ { |val|
    if (val != hoverIndex) {
      hoverIndex = val;
      template.changed(\hoverIndex);
    };
  }

  env_ { |val|
    env = val;
    template.changed(\env);
  }

  min_ { |val|
    if (val != min) {
      min = val;
      template.changed(\env);
    };
  }

  max_ { |val|
    if (val != max) {
      max = val;
      template.changed(\env);
    };
  }

  curve_ { |val|
    if (val != curve) {
      curve = val;
      template.changed(\env);
    };
  }

  isExponential_ { |val|
    if (val != isExponential) {
      isExponential = val;
      template.changed(\env);
    };
  }

  storeArgs { ^[env, min, max, curve, isExponential]; }

  *new { |env, min = 0, max = 1, curve = 0, isExponential = false|
    ^super.newCopyArgs(env, min, max, curve, isExponential);
  }

  prDraw { |aleft, atop, awidth, aheight, apratio, atratio, aenvHeight, astartTime|
    var points, image;
    var valStringFunc = { |val|
      // this is a real hack
      (if ((name == "level") or: (name.beginsWith("pre") or: (name.beginsWith("post")))) { val.ampdb.round(0.01).asString ++ " dB" } { val.round(0.01).asString });
    };
    var minString = valStringFunc.(min);
    var maxString = valStringFunc.(max);
    left = aleft;
    top = atop;
    width = awidth;
    height = aheight;
    pratio = apratio;
    tratio = atratio;
    envHeight = aenvHeight;
    startTime = astartTime;

    image = Image((width.asInteger)@((height + 1).asInteger));
    width.asInteger.do { |x|
      var x2time = { |x| x * pratio + startTime };
      var val2y = { |val| ((1 - val) * height).asInteger };
      var time = x2time.(x);
      var nextTime = x2time.(x + 1);
      var val = env[time];
      var nextVal = env[nextTime];
      var y = val2y.(val);
      var nextY = val2y.(nextVal);
      var thisTop = min(y, nextY);
      var thisHeight = max(1, abs(y - nextY));
      image.setPixels(Int32Array.fill(thisHeight, {Image.colorToPixel(Color.gray(0.6))}), Rect(x, thisTop, 1, thisHeight));
    };
    Pen.drawImage(left@top, image);
    image.free;

    // draw breakpoints
    points = this.envBreakPoints;
    points.do { |point, i|
      if (i == hoverIndex) {
        var val = this.valueAtIndex(i);
        var valString = valStringFunc.(val);
        Pen.stringAtPoint(valString, (point.x + 8)@(if (point.y - 12 < top) { point.y + 12 } { point.y - 13 }), Font.sansSerif(12), Color.gray(0.5));
        Pen.fillColor = Color.white;
        Pen.strokeColor = Color.gray(0.5);
        Pen.addOval(Rect(point.x - 2.5, point.y - 2.5, 6, 6));
        Pen.width = 2;
        Pen.fillStroke;
      } {
        Pen.addOval(Rect(point.x - 1.5, point.y - 1.5, 4, 4));
        Pen.fillColor = Color.gray(0.5);
        Pen.fill;
      };
    };

    if (height > 30) {
      Pen.stringAtPoint(maxString, (left + 2)@(top + 2), Font.sansSerif(10), Color.gray(0.8));
      Pen.stringAtPoint(minString, (left + 2)@(top + height - 10), Font.sansSerif(10), Color.gray(0.8));
    };
  }

  prHover { |x, y, hoverTime|
    var points = this.envBreakPoints;
    this.hoverIndex = this.nearestBreakPointIndex(points, (x@y));
  }

  prHoverLeave {
    this.hoverIndex = nil;
  }

  prMouseDown { |x, y, mods, buttNum, clickCount|
    editingFirst = false;
    originalCurve = nil;
    curveIndex = nil;

    if (mods.isShift) {
      var thisTime = ((x - left) * pratio) + startTime;
      this.addBreakPoint(thisTime);
    };

    if (mods.isAlt and: hoverIndex.notNil) {
      var thisEnv = env;
      var levels = [if (hoverIndex > 0) { thisEnv.levels[0] } { thisEnv.levels[1] }];
      var times = [];
      var curves = [];
      var time = 0;
      var addTime;
      thisEnv.times.do { |timeDiff, i|
        var curve = if (thisEnv.curves.isArray) { thisEnv.curves[i] } { thisEnv.curves };
        var level = thisEnv.levels[i + 1];
        if (i == (hoverIndex - 1)) {
          addTime = timeDiff;
        } {
          if (addTime.notNil) {
            timeDiff = timeDiff + addTime;
            addTime = nil;
          };
          levels = levels.add(level);
          times = times.add(timeDiff);
          curves = curves.add(curve);
        };
      };
      hoverIndex = nil;
      env = Env(levels, times, curves);
      template.changed(\env);
    };
  }

  prMouseMove { |x, y, xDelta, yDelta, mods|
    try {
      var thisEnv = env;
      var points;
      if (hoverIndex == 0) {
        env = Env([thisEnv.levels[0]] ++ thisEnv.levels, [0] ++ thisEnv.times, if (thisEnv.curves.isArray) { [thisEnv.curves[0]] ++ thisEnv.curves } { thisEnv.curves });
        hoverIndex = 1;
        editingFirst = true;
        thisEnv = env;
      };
      points = this.envBreakPoints;
      if (hoverIndex.notNil) {
        var newEnv, offset;
        // adjust breakpoint
        var prevPoint = points[max(0, hoverIndex - 1)];
        var nextPoint = if (hoverIndex < (points.size - 1)) { points[hoverIndex + 1] } { (left + width)@0 };
        var adjustedX = x.clip(prevPoint.x, nextPoint.x).clip(left, left + width);
        var adjustedY = y.clip(top, top + height);
        points[hoverIndex] = adjustedX@adjustedY;
        if (editingFirst) { points[0] = left@adjustedY; };
        this.env = this.envFromBreakPoints(points);
      } {
        // adjust curve if not over breakpoint and no modifiers
        if (mods == 0) {
          var curves = if (thisEnv.curves.isArray) { thisEnv.curves } { thisEnv.curves.dup(thisEnv.times.size) };
          curveIndex = curveIndex ?? this.segmentIndex(points, x@y);
          originalCurve = originalCurve ?? curves[curveIndex];
          if (originalCurve.isNumber) {
            var slope = thisEnv.levels[curveIndex + 1] - thisEnv.levels[curveIndex];
            curves[curveIndex] = originalCurve + (yDelta * 0.1 * slope.sign);
            this.env = Env(thisEnv.levels, thisEnv.times, curves);
          };
        };
      };
      template.changed(\env);
    };
  }

  envBreakPoints {
    var time = startTime * -1;
    var points = [];
    env.levels.do { |level, i|
      var x = left + (time * tratio);
      var y = top + ((1 - level) * height);
      points = points.add(x@y);
      if (i < env.times.size) {
        time = time + env.times[i];
      };
    };
    ^points;
  }

  envFromBreakPoints { |points|
    var curves = env.curves;
    var times = [];
    var levels = [1 - ((points[0].y - top) / height)];
    var offset = (points[0].x - left) * pratio;
    var time = startTime * -1;
    points[1..].do { |point|
      var timeDiff = ((point.x - left) * pratio) - time;
      var level = 1 - ((point.y - top) / height);
      times = times.add(timeDiff);
      levels = levels.add(level);
      time = time + timeDiff;
    }
    ^Env(levels, times, curves);
  }

  nearestBreakPointIndex { |points, thisPoint, tolerance = 5|
    var nearestIndex = nil;
    var nearestDistance = inf;
    points.do { |point, i|
      var distance = dist(point, thisPoint);
      if ((distance < tolerance) and: (distance < nearestDistance)) {
        nearestIndex = i;
      };
    };
    ^nearestIndex;
  }

  valueAtIndex { |i|
    ^this.prValueScale(env.levels[i]);
  }

  valueAtTime { |time|
    ^this.prValueScale(env.at(time));
  }

  prValueScale { |level|
    if (isExponential) {
      ^level.linexp(0.0, 1.0, min, max);
    } {
      ^level.lincurve(0.0, 1.0, min, max, curve);
    };
  }

  prValueUnscale { |value|
    if (isExponential) {
      ^value.explin(min, max, 0.0, 1.0);
    } {
      ^value.curvelin(min, max, 0.0, 1.0, curve);
    };
  }

  segmentIndex { |points, thisPoint|
    points[1..].do { |point, i|
      if ((points[i].x < thisPoint.x) and: (point.x > thisPoint.x)) { ^i };
    };
  }

  envToPlay { |startOffset = 0, duration, addFinalBreakpoint = false|
    var thisEnv = env;
    var playOffset = startOffset;
    var initlevel = thisEnv[playOffset];
    var endLevel;

    var levels = thisEnv.levels;
    var times = thisEnv.times;
    var curves = thisEnv.curves;

    var thisDuration = (duration ?? env.duration) - startOffset;

    if (times.size >= levels.size) {
      times = times[0..levels.size - 2];
    };

    if (playOffset < 0) {
      levels = [levels[0]] ++ levels;
      times = [playOffset * -1] ++ times;
      curves = if (curves.isArray) { [curves[0]] ++ curves } { curves };
    };
    if (playOffset > 0) {
      var runningtime = 0.0;
      var i = 0;
      while { (runningtime < playOffset) && (i < times.size) } {
        runningtime = runningtime + times[i];
        i = i + 1;
      };
      if (runningtime >= playOffset) { // passed target
        var timetokeep = runningtime - playOffset;
        levels = levels[(i - 1)..levels.size];
        times = times[(i - 1)..times.size];
        curves = if (curves.isArray) { curves[(i - 1)..curves.size] } { curves };
        times[0] = timetokeep;
        levels[0] = initlevel;
      } { // ran out of envelope
        times = [0];
        levels = initlevel ! 2;
        curves = \lin;
      };
    };

    endLevel = Env(levels, times, curves)[thisDuration];

    if (times.sum > thisDuration) {
      var prevTime = 0;
      var runningtime = 0.0;
      var i = 0;
      while { (runningtime < thisDuration) && (i < times.size) } {
        prevTime = runningtime;
        runningtime = runningtime + times[i];
        i = i + 1;
      };
      if (i > 0) {
        i = i - 1;
      };
      levels = levels[0..i+1];
      times = times[0..i];
      curves = if (curves.isArray) { curves[0..i] } { curves };

      times[i] = thisDuration - prevTime;
      levels[i+1] = endLevel;
    };

    if (addFinalBreakpoint) {
      if (times.sum < thisDuration) {
        levels = levels.add(endLevel);
        times = times.add(thisDuration - times.sum);
        if (curves.isArray) { curves = curves.add(curves.last) };
      };
    };

    ^Env(levels, times, curves);
  }

  getEnvAndDefName { |startTime, duration|
    var thisEnv = this.envToPlay(startTime, duration, true);
    var size = thisEnv.levels.size.nextPowerOfTwo;
    var defName = 'ESEnvClip_kr';
    if (size > 512) {
      "WARNING: Envelope can have max 512 points. Please adjust.".postln;
      size = 512;
    };
    defName = (defName ++ if (isExponential) { "_exp_" } { "_curve_" } ++ size).asSymbol;
    ^[thisEnv, defName];
  }

  defName {
    if (isExponential) {
      ^'ESEnvClip_kr_exp_2'
    } {
      ^'ESEnvClip_kr_curve_2'
    };
  }

  playPan { |startTime = 0.0, clock, mc, duration|
    var thisEnv, thisDefName;
    #thisEnv, thisDefName = this.getEnvAndDefName(startTime, duration);
    playRout = {
      thisEnv.times.do { |time, index|
        if (time > 0) {
          var levels = thisEnv.levels[index..index+1];
          var curves = if (thisEnv.curves.isArray) { thisEnv.curves[index] } { thisEnv.curves };
          Server.default.bind {
            synth.release;
            synth = mc.panAuto(this.defName, [env: Env(levels, [time], curves), tempo: clock.tempo, min: min, max: max, curve: curve]);
          };
          time.wait;
        };
      };
    }.fork(clock);
  }

  playLevel { |startTime = 0.0, clock, mc, duration|
    var thisEnv, thisDefName;
    #thisEnv, thisDefName = this.getEnvAndDefName(startTime, duration);
    playRout = {
      thisEnv.times.do { |time, index|
        if (time > 0) {
          var levels = thisEnv.levels[index..index+1];
          var curves = if (thisEnv.curves.isArray) { thisEnv.curves[index] } { thisEnv.curves };
          Server.default.bind {
            synth.release;
            synth = mc.levelAuto(this.defName, [env: Env(levels, [time], curves), tempo: clock.tempo, min: min, max: max, curve: curve]);
          };
          time.wait;
        };
      };
    }.fork(clock);
  }

  playSend { |startTime = 0.0, clock, mc, duration|
    var thisEnv, thisDefName, method, index;
    #thisEnv, thisDefName = this.getEnvAndDefName(startTime, duration);
    #method, index = name.split($_);
    index = index.interpret;
    method = if (method == "pre") { \preSends } { \postSends };
    playRout = {
      thisEnv.times.do { |time, i|
        if (time > 0) {
          var levels = thisEnv.levels[i..i+1];
          var curves = if (thisEnv.curves.isArray) { thisEnv.curves[i] } { thisEnv.curves };
          Server.default.bind {
            synth.release;
            synth = mc.perform(method)[index].levelAuto(this.defName, [env: Env(levels, [time], curves), tempo: clock.tempo, min: min, max: max, curve: curve]);
          };
          time.wait;
        };
      };
    }.fork(clock);
  }

  playFx { |startTime = 0.0, clock, mc, duration|
    var thisEnv, thisDefName, method, index, param;
    #thisEnv, thisDefName = this.getEnvAndDefName(startTime, duration);
    #method, index, param = name.split($_);
    index = index.interpret;
    param = param.asSymbol;

    bus.free;
    bus = Bus.control(Server.default, 1);
    Server.default.bind {
      template.fxSynths[index].set(param, bus.asMap);
    };

    playRout = {
      thisEnv.times.do { |time, i|
        if (time > 0) {
          var levels = thisEnv.levels[i..i+1];
          var curves = if (thisEnv.curves.isArray) { thisEnv.curves[i] } { thisEnv.curves };
          Server.default.bind {
            synth.release;
            synth = Synth(this.defName, [env: Env(levels, [time], curves), tempo: clock.tempo, min: min, max: max, curve: curve, out: bus], mc.effectgroup);
          };
          time.wait;
        };
      };
    }.fork(clock);
  }

  stop {
    //(name ++ " stopped").postln;
    if (playRout.notNil) { playRout.stop; playRout = nil; };
    Server.default.bind {
      synth.release;
    };
    synth = nil;
    bus.free;
    bus = nil;
  }

  addBreakPoint { |thisTime = 0, thisLevel, notify = true, insertTime = 0|
    var thisEnv = env;
    var levels = [env.levels[0]];
    var times = [], curves = [];
    var time = 0, inserted = false;
    var curve, level;
    thisLevel = thisLevel ?? env[thisTime];

    thisEnv.times.do { |timeDiff, i|
      curve = if (thisEnv.curves.isArray) { thisEnv.curves[i] } { thisEnv.curves };
      level = thisEnv.levels[i + 1] ?? level;
      if (inserted.not) {
        if ((time + timeDiff) > thisTime) {
          var ratio = (thisTime - time) / timeDiff;
          levels = levels.add(thisLevel);
          times = times.add(thisTime - time);
          if (curve.isNumber) {
            curves = curves.add(curve * ratio);
            curves = curves.add(curve * (1 - ratio));
          } {
            curves = curves.add(curve);
            curves = curves.add(curve);
          };
          if (insertTime > 0) {
            levels = levels.add(thisLevel);
            times = times.add(insertTime);
            curves = curves.add(0);
          };
          levels = levels.add(level);
          times = times.add(time + timeDiff - thisTime);
          inserted = true;
        } {
          levels = levels.add(level);
          times = times.add(timeDiff);
          curves = curves.add(curve);
        };
        time = time + timeDiff;
      } {
        levels = levels.add(level);
        times = times.add(timeDiff);
        curves = curves.add(curve);
      }
    };
    if (inserted.not) {
      levels = levels.add(thisLevel);
      times = times.add(thisTime - time);
      curves = curves.add(curve);
    };
    env = Env(levels, times, curves);
    if (notify) {
      template.changed(\env);
    };
  }

  insertTime { |timeA, timeB|
    var thisStartTime, thisEndTime, thisDuration;
    #thisStartTime, thisEndTime = [timeA, timeB].sort;
    thisDuration = thisEndTime - thisStartTime;

    this.addBreakPoint(thisStartTime, insertTime: thisDuration);
  }

  deleteTime { |timeA, timeB|

  }
}